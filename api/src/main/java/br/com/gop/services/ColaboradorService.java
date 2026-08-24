package br.com.gop.services;

import br.com.gop.models.Colaborador;
import br.com.gop.models.ColaboradorRequest;
import br.com.gop.models.ColaboradorResponse;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.repositories.ColaboradorRepository;
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
public class ColaboradorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ColaboradorService.class);

	private final EmpresaService empresaService;
	private final EquipamentoService equipamentoService;
	private final ColaboradorRepository colaboradorRepository;
	private final AuditoriaService auditoriaService;

	public ColaboradorService(
		EmpresaService empresaService,
		EquipamentoService equipamentoService,
		ColaboradorRepository colaboradorRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.equipamentoService = equipamentoService;
		this.colaboradorRepository = colaboradorRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<ColaboradorResponse> listar(Long empresaId) {
		empresaService.buscarEntidade(empresaId);
		return colaboradorRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public ColaboradorResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public ColaboradorResponse criar(Long empresaId, ColaboradorRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipamento equipamento = equipamentoService.buscarEntidade(empresaId, request.equipamentoId());
		validarEquipamentoAtivo(equipamento);
		String matricula = request.matricula().strip().toUpperCase(Locale.ROOT);
		garantirMatriculaUnica(empresaId, matricula, null);

		Colaborador colaborador = new Colaborador();
		colaborador.setEmpresa(empresa);
		colaborador.setEquipamento(equipamento);
		colaborador.setNome(request.nome().strip());
		colaborador.setMatricula(matricula);
		colaborador.setAtivo(request.ativo() == null || request.ativo());
		colaboradorRepository.save(colaborador);

		auditoriaService.registrar(
			empresa,
			"COLABORADOR_CRIADO",
			"Colaborador",
			colaborador.getId(),
			Map.of("matricula", matricula, "equipamentoId", equipamento.getId())
		);
		LOGGER.info(
			"Colaborador criado: id={}, empresaId={}, matricula={}",
			colaborador.getId(),
			empresaId,
			matricula
		);
		return paraResponse(colaborador);
	}

	@Transactional
	public ColaboradorResponse atualizar(Long empresaId, Long id, ColaboradorRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Colaborador colaborador = buscarEntidade(empresaId, id);
		Equipamento equipamento = equipamentoService.buscarEntidade(empresaId, request.equipamentoId());
		validarEquipamentoAtivo(equipamento);
		String matricula = request.matricula().strip().toUpperCase(Locale.ROOT);
		garantirMatriculaUnica(empresaId, matricula, id);

		colaborador.setEquipamento(equipamento);
		colaborador.setNome(request.nome().strip());
		colaborador.setMatricula(matricula);
		if (request.ativo() != null) {
			colaborador.setAtivo(request.ativo());
		}
		colaboradorRepository.save(colaborador);

		auditoriaService.registrar(
			empresa,
			"COLABORADOR_ATUALIZADO",
			"Colaborador",
			id,
			new LinkedHashMap<>(Map.of("matricula", matricula, "equipamentoId", equipamento.getId()))
		);
		LOGGER.info("Colaborador atualizado: id={}, empresaId={}", id, empresaId);
		return paraResponse(colaborador);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Colaborador colaborador = buscarEntidade(empresaId, id);
		colaborador.setAtivo(false);
		colaboradorRepository.save(colaborador);
		auditoriaService.registrar(
			empresa,
			"COLABORADOR_DESATIVADO",
			"Colaborador",
			id,
			Map.of("matricula", colaborador.getMatricula())
		);
		LOGGER.info("Colaborador desativado: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Colaborador buscarEntidade(Long empresaId, Long id) {
		return colaboradorRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado"));
	}

	@Transactional(readOnly = true)
	public List<Colaborador> buscarEntidades(Long empresaId, List<Long> ids) {
		List<Colaborador> encontrados = colaboradorRepository.findByEmpresaIdAndIdIn(empresaId, ids);
		if (encontrados.size() != ids.stream().distinct().count()) {
			throw new RecursoNaoEncontradoException("Um ou mais colaboradores não foram encontrados");
		}
		return encontrados;
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private void validarEquipamentoAtivo(Equipamento equipamento) {
		if (!equipamento.isAtivo()) {
			throw new RegraNegocioException("O lutocar informado está inativo");
		}
	}

	private void garantirMatriculaUnica(Long empresaId, String matricula, Long idAtual) {
		boolean existe = idAtual == null
			? colaboradorRepository.existsByEmpresaIdAndMatriculaIgnoreCase(empresaId, matricula)
			: colaboradorRepository.existsByEmpresaIdAndMatriculaIgnoreCaseAndIdNot(
				empresaId, matricula, idAtual
			);
		if (existe) {
			throw new ConflitoNegocioException("Já existe colaborador com esta matrícula na empresa");
		}
	}

	private ColaboradorResponse paraResponse(Colaborador colaborador) {
		return new ColaboradorResponse(
			colaborador.getId(),
			colaborador.getNome(),
			colaborador.getMatricula(),
			colaborador.getEquipamento().getId(),
			colaborador.getEquipamento().getCodigo(),
			colaborador.isAtivo()
		);
	}
}
