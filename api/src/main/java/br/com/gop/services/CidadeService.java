package br.com.gop.services;

import br.com.gop.models.Cidade;
import br.com.gop.models.CidadeRequest;
import br.com.gop.models.CidadeResponse;
import br.com.gop.models.Empresa;
import br.com.gop.repositories.CidadeRepository;
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
public class CidadeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CidadeService.class);

	private final EmpresaService empresaService;
	private final CidadeRepository cidadeRepository;
	private final AuditoriaService auditoriaService;

	public CidadeService(
		EmpresaService empresaService,
		CidadeRepository cidadeRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.cidadeRepository = cidadeRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<CidadeResponse> listar(Long empresaId) {
		empresaService.buscarEntidade(empresaId);
		return cidadeRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public CidadeResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public CidadeResponse criar(Long empresaId, CidadeRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		String nome = request.nome().strip();
		String uf = normalizarUf(request.uf());
		garantirNomeUnico(empresaId, nome, uf, null);

		Cidade cidade = new Cidade();
		cidade.setEmpresa(empresa);
		cidade.setNome(nome);
		cidade.setUf(uf);
		cidade.setAtivo(request.ativo() == null || request.ativo());
		cidadeRepository.save(cidade);

		auditar(empresa, "CIDADE_CRIADA", cidade.getId(), Map.of("nome", nome, "uf", uf));
		LOGGER.info("Cidade criada: id={}, empresaId={}, nome={}", cidade.getId(), empresaId, nome);
		return paraResponse(cidade);
	}

	@Transactional
	public CidadeResponse atualizar(Long empresaId, Long id, CidadeRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Cidade cidade = buscarEntidade(empresaId, id);
		String nome = request.nome().strip();
		String uf = normalizarUf(request.uf() != null ? request.uf() : cidade.getUf());
		garantirNomeUnico(empresaId, nome, uf, id);

		cidade.setNome(nome);
		cidade.setUf(uf);
		if (request.ativo() != null) {
			cidade.setAtivo(request.ativo());
		}
		cidadeRepository.save(cidade);

		auditar(empresa, "CIDADE_ATUALIZADA", cidade.getId(), Map.of("nome", nome, "uf", uf));
		LOGGER.info("Cidade atualizada: id={}, empresaId={}", id, empresaId);
		return paraResponse(cidade);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Cidade cidade = buscarEntidade(empresaId, id);
		cidade.setAtivo(false);
		cidadeRepository.save(cidade);
		auditar(empresa, "CIDADE_DESATIVADA", id, Map.of("nome", cidade.getNome()));
		LOGGER.info("Cidade desativada: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Cidade buscarEntidade(Long empresaId, Long id) {
		return cidadeRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Cidade não encontrada"));
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private void garantirNomeUnico(Long empresaId, String nome, String uf, Long idAtual) {
		boolean existe = idAtual == null
			? cidadeRepository.existsByEmpresaIdAndNomeIgnoreCaseAndUfIgnoreCase(empresaId, nome, uf)
			: cidadeRepository.existsByEmpresaIdAndNomeIgnoreCaseAndUfIgnoreCaseAndIdNot(
				empresaId, nome, uf, idAtual
			);
		if (existe) {
			throw new ConflitoNegocioException("Já existe cidade com este nome e UF na empresa");
		}
	}

	private String normalizarUf(String uf) {
		if (uf == null || uf.isBlank()) {
			return "RJ";
		}
		return uf.strip().toUpperCase(Locale.ROOT);
	}

	private void auditar(Empresa empresa, String acao, Long id, Map<String, Object> payload) {
		auditoriaService.registrar(empresa, acao, "Cidade", id, new LinkedHashMap<>(payload));
	}

	private CidadeResponse paraResponse(Cidade cidade) {
		return new CidadeResponse(cidade.getId(), cidade.getNome(), cidade.getUf(), cidade.isAtivo());
	}
}
