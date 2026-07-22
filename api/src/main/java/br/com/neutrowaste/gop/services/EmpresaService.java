package br.com.neutrowaste.gop.services;

import br.com.neutrowaste.gop.models.Empresa;
import br.com.neutrowaste.gop.models.EmpresaRequest;
import br.com.neutrowaste.gop.models.EmpresaResponse;
import br.com.neutrowaste.gop.repositories.EmpresaRepository;
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
public class EmpresaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmpresaService.class);

	private final EmpresaRepository empresaRepository;
	private final AuditoriaService auditoriaService;

	public EmpresaService(
		EmpresaRepository empresaRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaRepository = empresaRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<EmpresaResponse> listar(Boolean ativo) {
		List<Empresa> empresas = ativo == null
			? empresaRepository.findAllByOrderByNomeAsc()
			: empresaRepository.findByAtivoOrderByNomeAsc(ativo);
		return empresas.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public EmpresaResponse buscar(Long id) {
		return paraResponse(buscarEntidade(id));
	}

	@Transactional
	public EmpresaResponse criar(EmpresaRequest request) {
		String codigo = normalizarCodigo(request.codigo());
		if (empresaRepository.existsByCodigoIgnoreCase(codigo)) {
			throw new ConflitoNegocioException("Já existe uma empresa com o código informado");
		}

		Empresa empresa = new Empresa();
		empresa.setCodigo(codigo);
		empresa.setNome(request.nome().strip());
		empresa.setAtivo(request.ativo() == null || request.ativo());
		empresaRepository.save(empresa);

		Map<String, Object> payload = payloadEmpresa(empresa);
		auditoriaService.registrar(empresa, "EMPRESA_CRIADA", "Empresa", empresa.getId(), payload);
		LOGGER.info(
			"Empresa criada: empresaId={}, codigo={}, ativo={}",
			empresa.getId(),
			empresa.getCodigo(),
			empresa.isAtivo()
		);
		return paraResponse(empresa);
	}

	@Transactional
	public EmpresaResponse atualizar(Long id, EmpresaRequest request) {
		Empresa empresa = buscarEntidade(id);
		String codigo = normalizarCodigo(request.codigo());
		if (empresaRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
			throw new ConflitoNegocioException("Já existe uma empresa com o código informado");
		}

		Map<String, Object> anterior = payloadEmpresa(empresa);
		empresa.setCodigo(codigo);
		empresa.setNome(request.nome().strip());
		if (request.ativo() != null) {
			empresa.setAtivo(request.ativo());
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("anterior", anterior);
		payload.put("novo", payloadEmpresa(empresa));
		auditoriaService.registrar(
			empresa,
			"EMPRESA_ATUALIZADA",
			"Empresa",
			empresa.getId(),
			payload
		);
		LOGGER.info(
			"Empresa atualizada: empresaId={}, codigo={}, ativo={}",
			empresa.getId(),
			empresa.getCodigo(),
			empresa.isAtivo()
		);
		return paraResponse(empresa);
	}

	@Transactional
	public void desativar(Long id) {
		Empresa empresa = buscarEntidade(id);
		if (!empresa.isAtivo()) {
			return;
		}
		empresa.setAtivo(false);
		auditoriaService.registrar(
			empresa,
			"EMPRESA_DESATIVADA",
			"Empresa",
			empresa.getId(),
			payloadEmpresa(empresa)
		);
		LOGGER.info("Empresa desativada: empresaId={}, codigo={}", empresa.getId(), empresa.getCodigo());
	}

	@Transactional(readOnly = true)
	public Empresa buscarEntidade(Long id) {
		return empresaRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada"));
	}

	private EmpresaResponse paraResponse(Empresa empresa) {
		return new EmpresaResponse(
			empresa.getId(),
			empresa.getCodigo(),
			empresa.getNome(),
			empresa.isAtivo(),
			empresa.getCriadoEm(),
			empresa.getAtualizadoEm()
		);
	}

	private Map<String, Object> payloadEmpresa(Empresa empresa) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("codigo", empresa.getCodigo());
		payload.put("nome", empresa.getNome());
		payload.put("ativo", empresa.isAtivo());
		return payload;
	}

	private String normalizarCodigo(String codigo) {
		return codigo.strip().toUpperCase(Locale.ROOT);
	}
}
