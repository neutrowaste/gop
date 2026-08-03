package br.com.gop.services;

import br.com.gop.models.Colaborador;
import br.com.gop.models.ColaboradorRequest;
import br.com.gop.models.ColaboradorResponse;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipe;
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
	private final EquipeService equipeService;
	private final ColaboradorRepository colaboradorRepository;
	private final AuditoriaService auditoriaService;

	public ColaboradorService(
		EmpresaService empresaService,
		EquipeService equipeService,
		ColaboradorRepository colaboradorRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.equipeService = equipeService;
		this.colaboradorRepository = colaboradorRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<ColaboradorResponse> listar(Long empresaId, Long equipeId) {
		empresaService.buscarEntidade(empresaId);
		List<Colaborador> lista = equipeId == null
			? colaboradorRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			: colaboradorRepository.findByEmpresaIdAndEquipeIdOrderByNomeAsc(empresaId, equipeId);
		return lista.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public ColaboradorResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public ColaboradorResponse criar(Long empresaId, ColaboradorRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipe equipe = equipeService.buscarEntidade(empresaId, request.equipeId());
		String matricula = request.matricula().strip().toUpperCase(Locale.ROOT);
		garantirMatriculaUnica(empresaId, matricula, null);

		Colaborador colaborador = new Colaborador();
		colaborador.setEmpresa(empresa);
		colaborador.setEquipe(equipe);
		colaborador.setNome(request.nome().strip());
		colaborador.setMatricula(matricula);
		colaborador.setAtivo(request.ativo() == null || request.ativo());
		colaboradorRepository.save(colaborador);

		auditoriaService.registrar(
			empresa,
			"COLABORADOR_CRIADO",
			"Colaborador",
			colaborador.getId(),
			Map.of("matricula", matricula, "equipeId", equipe.getId())
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
		Equipe equipe = equipeService.buscarEntidade(empresaId, request.equipeId());
		String matricula = request.matricula().strip().toUpperCase(Locale.ROOT);
		garantirMatriculaUnica(empresaId, matricula, id);

		colaborador.setEquipe(equipe);
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
			new LinkedHashMap<>(Map.of("matricula", matricula, "equipeId", equipe.getId()))
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

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
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
			colaborador.getEquipe().getId(),
			colaborador.getEquipe().getNome(),
			colaborador.getNome(),
			colaborador.getMatricula(),
			colaborador.isAtivo()
		);
	}
}
