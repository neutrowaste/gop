package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.EquipamentoRequest;
import br.com.gop.models.EquipamentoResponse;
import br.com.gop.models.Equipe;
import br.com.gop.repositories.EquipamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("postgres")
public class EquipamentoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EquipamentoService.class);

	private final EmpresaService empresaService;
	private final EquipeService equipeService;
	private final EquipamentoRepository equipamentoRepository;
	private final ControleTetoAtivosService controleTetoAtivosService;
	private final AuditoriaService auditoriaService;

	public EquipamentoService(
		EmpresaService empresaService,
		EquipeService equipeService,
		EquipamentoRepository equipamentoRepository,
		ControleTetoAtivosService controleTetoAtivosService,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.equipeService = equipeService;
		this.equipamentoRepository = equipamentoRepository;
		this.controleTetoAtivosService = controleTetoAtivosService;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<EquipamentoResponse> listar(Long empresaId, Long equipeId) {
		empresaService.buscarEntidade(empresaId);
		List<Equipamento> lista = equipeId == null
			? equipamentoRepository.findByEmpresaIdOrderByCodigoAsc(empresaId)
			: equipamentoRepository.findByEmpresaIdAndEquipeIdOrderByCodigoAsc(empresaId, equipeId);
		return lista.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public EquipamentoResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public EquipamentoResponse criar(Long empresaId, EquipamentoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipe equipe = equipeService.buscarEntidade(empresaId, request.equipeId());
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, null);

		boolean ficaraAtivo = request.ativo() == null || request.ativo();
		if (ficaraAtivo) {
			controleTetoAtivosService.validarNovoAtivo(empresaId);
		}

		Equipamento equipamento = new Equipamento();
		equipamento.setEmpresa(empresa);
		equipamento.setEquipe(equipe);
		equipamento.setCodigo(codigo);
		equipamento.setDescricao(request.descricao() == null ? null : request.descricao().strip());
		equipamento.setAtivo(ficaraAtivo);
		equipamentoRepository.save(equipamento);

		auditoriaService.registrar(
			empresa,
			"EQUIPAMENTO_CRIADO",
			"Equipamento",
			equipamento.getId(),
			Map.of("codigo", codigo, "equipeId", equipe.getId())
		);
		LOGGER.info("Equipamento criado: id={}, empresaId={}, codigo={}", equipamento.getId(), empresaId, codigo);
		return paraResponse(equipamento);
	}

	@Transactional
	public EquipamentoResponse atualizar(Long empresaId, Long id, EquipamentoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipamento equipamento = buscarEntidade(empresaId, id);
		Equipe equipe = equipeService.buscarEntidade(empresaId, request.equipeId());
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, id);

		boolean reativando = request.ativo() != null && request.ativo() && !equipamento.isAtivo();
		if (reativando) {
			controleTetoAtivosService.validarNovoAtivo(empresaId);
		}

		equipamento.setEquipe(equipe);
		equipamento.setCodigo(codigo);
		equipamento.setDescricao(request.descricao() == null ? null : request.descricao().strip());
		if (request.ativo() != null) {
			equipamento.setAtivo(request.ativo());
		}
		equipamentoRepository.save(equipamento);

		auditoriaService.registrar(
			empresa,
			"EQUIPAMENTO_ATUALIZADO",
			"Equipamento",
			id,
			new LinkedHashMap<>(Map.of("codigo", codigo, "equipeId", equipe.getId()))
		);
		LOGGER.info("Equipamento atualizado: id={}, empresaId={}", id, empresaId);
		return paraResponse(equipamento);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipamento equipamento = buscarEntidade(empresaId, id);
		equipamento.setAtivo(false);
		equipamentoRepository.save(equipamento);
		auditoriaService.registrar(
			empresa,
			"EQUIPAMENTO_DESATIVADO",
			"Equipamento",
			id,
			Map.of("codigo", equipamento.getCodigo())
		);
		LOGGER.info("Equipamento desativado: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Equipamento buscarEntidade(Long empresaId, Long id) {
		return equipamentoRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado"));
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private void garantirCodigoUnico(Long empresaId, String codigo, Long idAtual) {
		boolean existe = idAtual == null
			? equipamentoRepository.existsByEmpresaIdAndCodigoIgnoreCase(empresaId, codigo)
			: equipamentoRepository.existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(empresaId, codigo, idAtual);
		if (existe) {
			throw new ConflitoNegocioException("Já existe equipamento com este código na empresa");
		}
	}

	private EquipamentoResponse paraResponse(Equipamento equipamento) {
		return new EquipamentoResponse(
			equipamento.getId(),
			equipamento.getEquipe().getId(),
			equipamento.getEquipe().getNome(),
			equipamento.getCodigo(),
			equipamento.getDescricao(),
			equipamento.isAtivo()
		);
	}
}
