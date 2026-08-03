package br.com.gop.services;

import br.com.gop.models.Cidade;
import br.com.gop.models.Empresa;
import br.com.gop.models.Setor;
import br.com.gop.models.SetorRequest;
import br.com.gop.models.SetorResponse;
import br.com.gop.repositories.SetorRepository;
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
public class SetorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetorService.class);

	private final EmpresaService empresaService;
	private final CidadeService cidadeService;
	private final SetorRepository setorRepository;
	private final AuditoriaService auditoriaService;

	public SetorService(
		EmpresaService empresaService,
		CidadeService cidadeService,
		SetorRepository setorRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.cidadeService = cidadeService;
		this.setorRepository = setorRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<SetorResponse> listar(Long empresaId, Long cidadeId) {
		empresaService.buscarEntidade(empresaId);
		List<Setor> setores = cidadeId == null
			? setorRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			: setorRepository.findByEmpresaIdAndCidadeIdOrderByNomeAsc(empresaId, cidadeId);
		return setores.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public SetorResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public SetorResponse criar(Long empresaId, SetorRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Cidade cidade = cidadeService.buscarEntidade(empresaId, request.cidadeId());
		String codigo = normalizarCodigo(request.codigo());
		if (codigo != null) {
			garantirCodigoUnico(empresaId, codigo, null);
		}

		Setor setor = new Setor();
		setor.setEmpresa(empresa);
		setor.setCidade(cidade);
		setor.setCodigo(codigo);
		setor.setNome(request.nome().strip());
		setor.setAtivo(request.ativo() == null || request.ativo());
		setorRepository.save(setor);

		auditoriaService.registrar(
			empresa,
			"SETOR_CRIADO",
			"Setor",
			setor.getId(),
			Map.of("nome", setor.getNome(), "codigo", codigo == null ? "" : codigo, "cidadeId", cidade.getId())
		);
		LOGGER.info("Setor criado: id={}, empresaId={}, codigo={}", setor.getId(), empresaId, codigo);
		return paraResponse(setor);
	}

	@Transactional
	public SetorResponse atualizar(Long empresaId, Long id, SetorRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = buscarEntidade(empresaId, id);
		Cidade cidade = cidadeService.buscarEntidade(empresaId, request.cidadeId());
		String codigo = normalizarCodigo(request.codigo());
		if (codigo != null) {
			garantirCodigoUnico(empresaId, codigo, id);
		}

		setor.setCidade(cidade);
		setor.setCodigo(codigo);
		setor.setNome(request.nome().strip());
		if (request.ativo() != null) {
			setor.setAtivo(request.ativo());
		}
		setorRepository.save(setor);

		auditoriaService.registrar(
			empresa,
			"SETOR_ATUALIZADO",
			"Setor",
			setor.getId(),
			new LinkedHashMap<>(Map.of("nome", setor.getNome(), "cidadeId", cidade.getId()))
		);
		LOGGER.info("Setor atualizado: id={}, empresaId={}", id, empresaId);
		return paraResponse(setor);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = buscarEntidade(empresaId, id);
		setor.setAtivo(false);
		setorRepository.save(setor);
		auditoriaService.registrar(empresa, "SETOR_DESATIVADO", "Setor", id, Map.of("nome", setor.getNome()));
		LOGGER.info("Setor desativado: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Setor buscarEntidade(Long empresaId, Long id) {
		return setorRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado"));
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
			? setorRepository.existsByEmpresaIdAndCodigoIgnoreCase(empresaId, codigo)
			: setorRepository.existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(empresaId, codigo, idAtual);
		if (existe) {
			throw new ConflitoNegocioException("Já existe setor com este código na empresa");
		}
	}

	private String normalizarCodigo(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return null;
		}
		return codigo.strip().toUpperCase(Locale.ROOT);
	}

	private SetorResponse paraResponse(Setor setor) {
		return new SetorResponse(
			setor.getId(),
			setor.getCidade().getId(),
			setor.getCidade().getNome(),
			setor.getCodigo(),
			setor.getNome(),
			setor.isAtivo()
		);
	}
}
