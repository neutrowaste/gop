package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.Ocorrencia;
import br.com.gop.models.OcorrenciaResolucaoRequest;
import br.com.gop.models.OcorrenciaResponse;
import br.com.gop.models.StatusOcorrencia;
import br.com.gop.models.TipoOcorrencia;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.Usuario;
import br.com.gop.repositories.OcorrenciaRepository;
import br.com.gop.repositories.UsuarioRepository;
import br.com.gop.security.SegurancaContexto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ocorrências automáticas do motor de Varrição (RN-001): ordem do plano
 * pulada e GPS em silêncio. Resolução/justificativa fica auditada
 * (mesmo padrão de AuditoriaService dos demais módulos).
 */
@Service
@Profile("postgres")
public class OcorrenciaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OcorrenciaService.class);

	private static final Sort ORDENACAO_PADRAO = Sort.by(Sort.Order.desc("detectadaEm"));

	private final EmpresaService empresaService;
	private final UsuarioRepository usuarioRepository;
	private final OcorrenciaRepository ocorrenciaRepository;
	private final AuditoriaService auditoriaService;

	public OcorrenciaService(
		EmpresaService empresaService,
		UsuarioRepository usuarioRepository,
		OcorrenciaRepository ocorrenciaRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.usuarioRepository = usuarioRepository;
		this.ocorrenciaRepository = ocorrenciaRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<OcorrenciaResponse> listar(
		Long empresaId,
		LocalDate data,
		Long setorId,
		Long equipeId,
		StatusOcorrencia status,
		TipoOcorrencia tipo
	) {
		empresaService.buscarEntidade(empresaId);
		Specification<Ocorrencia> filtro = comFiltros(empresaId, data, setorId, equipeId, status, tipo);
		return ocorrenciaRepository.findAll(filtro, ORDENACAO_PADRAO)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	private Specification<Ocorrencia> comFiltros(
		Long empresaId,
		LocalDate data,
		Long setorId,
		Long equipeId,
		StatusOcorrencia status,
		TipoOcorrencia tipo
	) {
		return (root, query, cb) -> {
			List<Predicate> predicados = new ArrayList<>();
			predicados.add(cb.equal(root.get("empresa").get("id"), empresaId));
			Join<Object, Object> escala = root.join("escala");
			if (data != null) {
				predicados.add(cb.equal(escala.get("data"), data));
			}
			if (setorId != null) {
				predicados.add(cb.equal(escala.get("setor").get("id"), setorId));
			}
			if (equipeId != null) {
				predicados.add(cb.equal(escala.get("equipe").get("id"), equipeId));
			}
			if (status != null) {
				predicados.add(cb.equal(root.get("status"), status));
			}
			if (tipo != null) {
				predicados.add(cb.equal(root.get("tipo"), tipo));
			}
			return cb.and(predicados.toArray(new Predicate[0]));
		};
	}

	@Transactional(readOnly = true)
	public OcorrenciaResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public OcorrenciaResponse resolver(Long empresaId, Long id, OcorrenciaResolucaoRequest request) {
		if (request.status() != StatusOcorrencia.RESOLVIDA && request.status() != StatusOcorrencia.JUSTIFICADA) {
			throw new RegraNegocioException("Status de resolução deve ser RESOLVIDA ou JUSTIFICADA");
		}
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		Ocorrencia ocorrencia = buscarEntidade(empresaId, id);
		if (ocorrencia.getStatus() != StatusOcorrencia.ABERTA) {
			throw new RegraNegocioException("Ocorrência já foi tratada");
		}
		ocorrencia.setStatus(request.status());
		ocorrencia.setResolvidaEm(Instant.now());
		ocorrencia.setObservacaoResolucao(request.observacao());
		Long usuarioId = SegurancaContexto.usuarioIdAtual();
		if (usuarioId != null) {
			ocorrencia.setResolvidaPorUsuario(usuarioRepository.getReferenceById(usuarioId));
		}
		ocorrenciaRepository.save(ocorrencia);

		auditoriaService.registrar(
			empresa,
			request.status() == StatusOcorrencia.JUSTIFICADA ? "OCORRENCIA_JUSTIFICADA" : "OCORRENCIA_RESOLVIDA",
			"Ocorrencia",
			id,
			Map.of("tipo", ocorrencia.getTipo().name(), "status", ocorrencia.getStatus().name())
		);
		LOGGER.info("Ocorrência {}: id={}, empresaId={}", ocorrencia.getStatus(), id, empresaId);
		return paraResponse(ocorrencia);
	}

	@Transactional(readOnly = true)
	public Ocorrencia buscarEntidade(Long empresaId, Long id) {
		return ocorrenciaRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência não encontrada"));
	}

	/**
	 * Chamado pelo motor de cobertura ao detectar que um trecho foi concluído
	 * fora da sequência planejada. Idempotente: não duplica ocorrência aberta
	 * para o mesmo trecho do plano.
	 */
	@Transactional
	public void criarOrdemViolada(Empresa empresa, Escala escala, TrechoExecucao trechoExecucao) {
		boolean jaExiste = ocorrenciaRepository.existsByTrechoExecucaoIdAndTipoAndStatus(
			trechoExecucao.getId(),
			TipoOcorrencia.ORDEM_PLANO_VIOLADA,
			StatusOcorrencia.ABERTA
		);
		if (jaExiste) {
			return;
		}
		Ocorrencia ocorrencia = new Ocorrencia();
		ocorrencia.setEmpresa(empresa);
		ocorrencia.setEscala(escala);
		ocorrencia.setEquipamento(escala.getEquipamento());
		ocorrencia.setTrechoExecucao(trechoExecucao);
		ocorrencia.setTipo(TipoOcorrencia.ORDEM_PLANO_VIOLADA);
		ocorrencia.setDescricao(
			"Trecho " + trechoExecucao.getTrecho().getCodigo()
				+ " (ordem " + trechoExecucao.getOrdem() + ") foi executado fora da sequência planejada do dia"
		);
		ocorrencia.setDetectadaEm(Instant.now());
		ocorrenciaRepository.save(ocorrencia);

		auditoriaService.registrar(
			empresa,
			"OCORRENCIA_ORDEM_VIOLADA_CRIADA",
			"Ocorrencia",
			ocorrencia.getId(),
			Map.of("escalaId", escala.getId(), "trechoExecucaoId", trechoExecucao.getId())
		);
		LOGGER.info(
			"Ocorrência ORDEM_PLANO_VIOLADA criada: escalaId={}, trechoExecucaoId={}",
			escala.getId(),
			trechoExecucao.getId()
		);
	}

	/**
	 * Chamado pelo SaudeGpsScheduler ao detectar silêncio de transmissão além
	 * do limiar configurado. Idempotente por (escala, equipamento).
	 */
	@Transactional
	public void criarGpsSilencioso(Empresa empresa, Escala escala, Equipamento equipamento, long minutosSemSinal) {
		boolean jaExiste = ocorrenciaRepository.existsByEscalaIdAndEquipamentoIdAndTipoAndStatus(
			escala.getId(),
			equipamento.getId(),
			TipoOcorrencia.GPS_SILENCIOSO,
			StatusOcorrencia.ABERTA
		);
		if (jaExiste) {
			return;
		}
		Ocorrencia ocorrencia = new Ocorrencia();
		ocorrencia.setEmpresa(empresa);
		ocorrencia.setEscala(escala);
		ocorrencia.setEquipamento(equipamento);
		ocorrencia.setTipo(TipoOcorrencia.GPS_SILENCIOSO);
		ocorrencia.setDescricao(
			"Ativo GPS " + equipamento.getCodigo() + " sem transmissão há " + minutosSemSinal + " minutos"
		);
		ocorrencia.setDetectadaEm(Instant.now());
		ocorrenciaRepository.save(ocorrencia);

		auditoriaService.registrar(
			empresa,
			"OCORRENCIA_GPS_SILENCIOSO_CRIADA",
			"Ocorrencia",
			ocorrencia.getId(),
			Map.of("escalaId", escala.getId(), "equipamentoId", equipamento.getId(), "minutos", minutosSemSinal)
		);
		LOGGER.info(
			"Ocorrência GPS_SILENCIOSO criada: escalaId={}, equipamentoId={}, minutos={}",
			escala.getId(),
			equipamento.getId(),
			minutosSemSinal
		);
	}

	/** Resolve automaticamente a ocorrência de GPS em silêncio quando o sinal é restabelecido. */
	@Transactional
	public void resolverAutomaticamenteGpsSilencioso(Long escalaId, Long equipamentoId) {
		ocorrenciaRepository.findByEscalaIdAndEquipamentoIdAndTipoAndStatus(
			escalaId,
			equipamentoId,
			TipoOcorrencia.GPS_SILENCIOSO,
			StatusOcorrencia.ABERTA
		).ifPresent(ocorrencia -> {
			ocorrencia.setStatus(StatusOcorrencia.RESOLVIDA);
			ocorrencia.setResolvidaEm(Instant.now());
			ocorrencia.setObservacaoResolucao("Sinal do GPS restabelecido automaticamente");
			ocorrenciaRepository.save(ocorrencia);
			LOGGER.info(
				"Ocorrência GPS_SILENCIOSO resolvida automaticamente: escalaId={}, equipamentoId={}",
				escalaId,
				equipamentoId
			);
		});
	}

	private OcorrenciaResponse paraResponse(Ocorrencia ocorrencia) {
		Escala escala = ocorrencia.getEscala();
		TrechoExecucao trechoExecucao = ocorrencia.getTrechoExecucao();
		return new OcorrenciaResponse(
			ocorrencia.getId(),
			escala.getId(),
			escala.getData(),
			escala.getSetor().getNome(),
			escala.getEquipe().getNome(),
			ocorrencia.getEquipamento().getId(),
			ocorrencia.getEquipamento().getCodigo(),
			trechoExecucao != null ? trechoExecucao.getId() : null,
			trechoExecucao != null ? trechoExecucao.getTrecho().getCodigo() : null,
			ocorrencia.getTipo(),
			ocorrencia.getDescricao(),
			ocorrencia.getStatus(),
			ocorrencia.getDetectadaEm(),
			ocorrencia.getResolvidaEm(),
			ocorrencia.getResolvidaPorUsuario() != null ? ocorrencia.getResolvidaPorUsuario().getNome() : null,
			ocorrencia.getObservacaoResolucao(),
			ocorrencia.isAtivo()
		);
	}
}
