package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Equipe;
import br.com.gop.models.Escala;
import br.com.gop.models.EscalaRequest;
import br.com.gop.models.EscalaResponse;
import br.com.gop.models.Setor;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.Usuario;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.UsuarioRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Amarração diária do módulo Escala (RN-001): pré-requisito do motor de
 * regras da Varrição, que só processa posições de escalas PUBLICADA.
 */
@Service
@Profile("postgres")
public class EscalaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscalaService.class);

	private static final Sort ORDENACAO_PADRAO = Sort.by(
		Sort.Order.desc("data"),
		Sort.Order.asc("turno"),
		Sort.Order.asc("setor.nome")
	);

	private final EmpresaService empresaService;
	private final SetorService setorService;
	private final EquipeService equipeService;
	private final EquipamentoService equipamentoService;
	private final UsuarioRepository usuarioRepository;
	private final EscalaRepository escalaRepository;
	private final AuditoriaService auditoriaService;

	public EscalaService(
		EmpresaService empresaService,
		SetorService setorService,
		EquipeService equipeService,
		EquipamentoService equipamentoService,
		UsuarioRepository usuarioRepository,
		EscalaRepository escalaRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.setorService = setorService;
		this.equipeService = equipeService;
		this.equipamentoService = equipamentoService;
		this.usuarioRepository = usuarioRepository;
		this.escalaRepository = escalaRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<EscalaResponse> listar(
		Long empresaId,
		LocalDate data,
		Long setorId,
		Long equipeId,
		StatusEscala status
	) {
		empresaService.buscarEntidade(empresaId);
		Specification<Escala> filtro = comFiltros(empresaId, data, setorId, equipeId, status);
		return escalaRepository.findAll(filtro, ORDENACAO_PADRAO)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	/**
	 * Usa Specification (em vez de "(:param is null or campo = :param)" em JPQL)
	 * porque o driver do PostgreSQL não consegue inferir o tipo do parâmetro
	 * quando ele só aparece em "is null", retornando erro em runtime.
	 */
	private Specification<Escala> comFiltros(
		Long empresaId,
		LocalDate data,
		Long setorId,
		Long equipeId,
		StatusEscala status
	) {
		return (root, query, cb) -> {
			List<Predicate> predicados = new ArrayList<>();
			predicados.add(cb.equal(root.get("empresa").get("id"), empresaId));
			if (data != null) {
				predicados.add(cb.equal(root.get("data"), data));
			}
			if (setorId != null) {
				predicados.add(cb.equal(root.get("setor").get("id"), setorId));
			}
			if (equipeId != null) {
				predicados.add(cb.equal(root.get("equipe").get("id"), equipeId));
			}
			if (status != null) {
				predicados.add(cb.equal(root.get("status"), status));
			}
			return cb.and(predicados.toArray(new Predicate[0]));
		};
	}

	@Transactional(readOnly = true)
	public EscalaResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public EscalaResponse criar(Long empresaId, EscalaRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());
		Equipe equipe = equipeService.buscarEntidade(empresaId, request.equipeId());
		Equipamento equipamento = equipamentoService.buscarEntidade(empresaId, request.equipamentoId());
		Usuario supervisor = buscarSupervisor(empresaId, request.supervisorUsuarioId());
		validarAmarracao(equipe, equipamento);
		garantirEquipamentoLivreNoTurno(empresaId, request.data(), request.turno(), request.equipamentoId(), null);

		Escala escala = new Escala();
		escala.setEmpresa(empresa);
		escala.setData(request.data());
		escala.setTurno(request.turno());
		escala.setSetor(setor);
		escala.setEquipe(equipe);
		escala.setEquipamento(equipamento);
		escala.setSupervisor(supervisor);
		escala.setStatus(StatusEscala.RASCUNHO);
		escala.setAtivo(request.ativo() == null || request.ativo());
		escalaRepository.save(escala);

		auditoriaService.registrar(
			empresa,
			"ESCALA_CRIADA",
			"Escala",
			escala.getId(),
			payloadBase(escala)
		);
		LOGGER.info(
			"Escala criada: id={}, empresaId={}, data={}, turno={}",
			escala.getId(),
			empresaId,
			escala.getData(),
			escala.getTurno()
		);
		return paraResponse(escala);
	}

	@Transactional
	public EscalaResponse atualizar(Long empresaId, Long id, EscalaRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		if (escala.getStatus() == StatusEscala.PUBLICADA) {
			throw new RegraNegocioException("Escala publicada não pode ser editada; despublique antes de alterar");
		}

		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());
		Equipe equipe = equipeService.buscarEntidade(empresaId, request.equipeId());
		Equipamento equipamento = equipamentoService.buscarEntidade(empresaId, request.equipamentoId());
		Usuario supervisor = buscarSupervisor(empresaId, request.supervisorUsuarioId());
		validarAmarracao(equipe, equipamento);
		garantirEquipamentoLivreNoTurno(empresaId, request.data(), request.turno(), request.equipamentoId(), id);

		escala.setData(request.data());
		escala.setTurno(request.turno());
		escala.setSetor(setor);
		escala.setEquipe(equipe);
		escala.setEquipamento(equipamento);
		escala.setSupervisor(supervisor);
		if (request.ativo() != null) {
			escala.setAtivo(request.ativo());
		}
		escalaRepository.save(escala);

		auditoriaService.registrar(empresa, "ESCALA_ATUALIZADA", "Escala", id, payloadBase(escala));
		LOGGER.info("Escala atualizada: id={}, empresaId={}", id, empresaId);
		return paraResponse(escala);
	}

	@Transactional
	public EscalaResponse publicar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		if (!escala.isAtivo()) {
			throw new RegraNegocioException("Escala cancelada não pode ser publicada");
		}
		if (escala.getStatus() == StatusEscala.PUBLICADA) {
			return paraResponse(escala);
		}
		escala.setStatus(StatusEscala.PUBLICADA);
		escala.setPublicadaEm(Instant.now());
		escalaRepository.save(escala);

		auditoriaService.registrar(empresa, "ESCALA_PUBLICADA", "Escala", id, payloadBase(escala));
		LOGGER.info("Escala publicada: id={}, empresaId={}", id, empresaId);
		return paraResponse(escala);
	}

	@Transactional
	public EscalaResponse despublicar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		if (escala.getStatus() == StatusEscala.RASCUNHO) {
			return paraResponse(escala);
		}
		escala.setStatus(StatusEscala.RASCUNHO);
		escala.setPublicadaEm(null);
		escalaRepository.save(escala);

		auditoriaService.registrar(empresa, "ESCALA_DESPUBLICADA", "Escala", id, payloadBase(escala));
		LOGGER.info("Escala despublicada: id={}, empresaId={}", id, empresaId);
		return paraResponse(escala);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		escala.setAtivo(false);
		escalaRepository.save(escala);
		auditoriaService.registrar(empresa, "ESCALA_CANCELADA", "Escala", id, payloadBase(escala));
		LOGGER.info("Escala cancelada: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Escala buscarEntidade(Long empresaId, Long id) {
		return escalaRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Escala não encontrada"));
	}

	private Usuario buscarSupervisor(Long empresaId, Long usuarioId) {
		Usuario supervisor = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Supervisor não encontrado"));
		if (!supervisor.isAtivo()) {
			throw new RegraNegocioException("Supervisor selecionado está inativo");
		}
		return supervisor;
	}

	private void validarAmarracao(Equipe equipe, Equipamento equipamento) {
		if (!equipamento.getEquipe().getId().equals(equipe.getId())) {
			throw new RegraNegocioException("O equipamento (Ativo GPS) informado pertence a outra equipe");
		}
		if (!equipamento.isAtivo()) {
			throw new RegraNegocioException("O equipamento (Ativo GPS) informado está inativo");
		}
		if (!equipe.isAtivo()) {
			throw new RegraNegocioException("A equipe informada está inativa");
		}
	}

	private void garantirEquipamentoLivreNoTurno(
		Long empresaId,
		LocalDate data,
		br.com.gop.models.Turno turno,
		Long equipamentoId,
		Long idAtual
	) {
		boolean ocupado = idAtual == null
			? escalaRepository.existsByEmpresaIdAndDataAndTurnoAndEquipamentoId(empresaId, data, turno, equipamentoId)
			: escalaRepository.existsByEmpresaIdAndDataAndTurnoAndEquipamentoIdAndIdNot(
				empresaId,
				data,
				turno,
				equipamentoId,
				idAtual
			);
		if (ocupado) {
			throw new ConflitoNegocioException(
				"Este equipamento (Ativo GPS) já está escalado nesta data e turno"
			);
		}
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private Map<String, Object> payloadBase(Escala escala) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("data", escala.getData().toString());
		payload.put("turno", escala.getTurno().name());
		payload.put("setorId", escala.getSetor().getId());
		payload.put("equipeId", escala.getEquipe().getId());
		payload.put("equipamentoId", escala.getEquipamento().getId());
		payload.put("supervisorUsuarioId", escala.getSupervisor().getId());
		payload.put("status", escala.getStatus().name());
		return payload;
	}

	private EscalaResponse paraResponse(Escala escala) {
		return new EscalaResponse(
			escala.getId(),
			escala.getData(),
			escala.getTurno(),
			escala.getSetor().getId(),
			escala.getSetor().getNome(),
			escala.getEquipe().getId(),
			escala.getEquipe().getNome(),
			escala.getEquipamento().getId(),
			escala.getEquipamento().getCodigo(),
			escala.getSupervisor().getId(),
			escala.getSupervisor().getNome(),
			escala.getStatus(),
			escala.getPublicadaEm(),
			escala.isAtivo()
		);
	}
}
