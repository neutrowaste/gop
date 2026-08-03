package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipe;
import br.com.gop.models.EquipeRequest;
import br.com.gop.models.EquipeResponse;
import br.com.gop.models.Setor;
import br.com.gop.repositories.EquipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("postgres")
public class EquipeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EquipeService.class);

	private final EmpresaService empresaService;
	private final SetorService setorService;
	private final EquipeRepository equipeRepository;
	private final AuditoriaService auditoriaService;

	public EquipeService(
		EmpresaService empresaService,
		SetorService setorService,
		EquipeRepository equipeRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.setorService = setorService;
		this.equipeRepository = equipeRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<EquipeResponse> listar(Long empresaId, Long setorId) {
		empresaService.buscarEntidade(empresaId);
		List<Equipe> equipes = setorId == null
			? equipeRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			: equipeRepository.findByEmpresaIdAndSetorIdOrderByNomeAsc(empresaId, setorId);
		return equipes.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public EquipeResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public EquipeResponse criar(Long empresaId, EquipeRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());

		Equipe equipe = new Equipe();
		equipe.setEmpresa(empresa);
		equipe.setSetor(setor);
		equipe.setNome(request.nome().strip());
		equipe.setAtivo(request.ativo() == null || request.ativo());
		equipeRepository.save(equipe);

		auditoriaService.registrar(
			empresa,
			"EQUIPE_CRIADA",
			"Equipe",
			equipe.getId(),
			Map.of("nome", equipe.getNome(), "setorId", setor.getId())
		);
		LOGGER.info("Equipe criada: id={}, empresaId={}, nome={}", equipe.getId(), empresaId, equipe.getNome());
		return paraResponse(equipe);
	}

	@Transactional
	public EquipeResponse atualizar(Long empresaId, Long id, EquipeRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipe equipe = buscarEntidade(empresaId, id);
		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());

		equipe.setSetor(setor);
		equipe.setNome(request.nome().strip());
		if (request.ativo() != null) {
			equipe.setAtivo(request.ativo());
		}
		equipeRepository.save(equipe);

		auditoriaService.registrar(
			empresa,
			"EQUIPE_ATUALIZADA",
			"Equipe",
			id,
			new LinkedHashMap<>(Map.of("nome", equipe.getNome(), "setorId", setor.getId()))
		);
		LOGGER.info("Equipe atualizada: id={}, empresaId={}", id, empresaId);
		return paraResponse(equipe);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipe equipe = buscarEntidade(empresaId, id);
		equipe.setAtivo(false);
		equipeRepository.save(equipe);
		auditoriaService.registrar(empresa, "EQUIPE_DESATIVADA", "Equipe", id, Map.of("nome", equipe.getNome()));
		LOGGER.info("Equipe desativada: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Equipe buscarEntidade(Long empresaId, Long id) {
		return equipeRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Equipe não encontrada"));
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private EquipeResponse paraResponse(Equipe equipe) {
		return new EquipeResponse(
			equipe.getId(),
			equipe.getSetor().getId(),
			equipe.getSetor().getNome(),
			equipe.getNome(),
			equipe.isAtivo()
		);
	}
}
