package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Setor;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoDesenhoRequest;
import br.com.gop.models.TrechoImportacaoItemResponse;
import br.com.gop.models.TrechoImportacaoResponse;
import br.com.gop.models.TrechoRequest;
import br.com.gop.models.TrechoResponse;
import br.com.gop.repositories.TrechoRepository;
import org.locationtech.jts.geom.LineString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Profile("postgres")
public class TrechoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TrechoService.class);
	private static final int MAX_TRECHOS_DESENHO = 500;

	private final EmpresaService empresaService;
	private final SetorService setorService;
	private final TrechoRepository trechoRepository;
	private final AuditoriaService auditoriaService;

	public TrechoService(
		EmpresaService empresaService,
		SetorService setorService,
		TrechoRepository trechoRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.setorService = setorService;
		this.trechoRepository = trechoRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<TrechoResponse> listar(Long empresaId, Long setorId) {
		empresaService.buscarEntidade(empresaId);
		List<Trecho> trechos = setorId == null
			? trechoRepository.findByEmpresaIdOrderByCodigoAsc(empresaId)
			: trechoRepository.findByEmpresaIdAndSetorIdOrderByCodigoAsc(empresaId, setorId);
		return trechos.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public TrechoResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public TrechoResponse criar(Long empresaId, TrechoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());
		if (!setor.isAtivo()) {
			throw new RegraNegocioException("Setor informado está inativo");
		}
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, null);
		LineString geom = GeometriaWkt.lineStringDeWkt(request.geomWkt());

		Trecho trecho = new Trecho();
		trecho.setEmpresa(empresa);
		trecho.setSetor(setor);
		trecho.setNomeRua(normalizarNomeRua(request.ruaNome()));
		trecho.setCodigo(codigo);
		trecho.setGeom(geom);
		aplicarCamposOperacionais(trecho, request, true);
		trecho.setAtivo(request.ativo() == null || request.ativo());
		trechoRepository.save(trecho);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("codigo", codigo);
		payload.put("setorId", setor.getId());
		payload.put("status", trecho.getStatus().name());
		auditoriaService.registrar(empresa, "TRECHO_CRIADO", "Trecho", trecho.getId(), payload);
		LOGGER.info("Trecho criado: id={}, empresaId={}, codigo={}", trecho.getId(), empresaId, codigo);
		return paraResponse(trecho);
	}

	@Transactional
	public TrechoImportacaoResponse importarShapefile(
		Long empresaId,
		Long setorId,
		MultipartFile arquivo,
		String atributoRua,
		String atributoCodigo,
		String charset
	) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = setorService.buscarEntidade(empresaId, setorId);
		if (!setor.isAtivo()) {
			throw new RegraNegocioException("Setor informado está inativo");
		}
		if (arquivo == null || arquivo.isEmpty()) {
			throw new RegraNegocioException("Envie um ZIP contendo o shapefile (.shp, .shx, .dbf)");
		}
		String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
		if (!nome.endsWith(".zip")) {
			throw new RegraNegocioException("Envie um arquivo .zip com o shapefile");
		}

		Path temp = null;
		try {
			temp = Files.createTempDirectory("gop-shp-");
			Path shp;
			try (var in = arquivo.getInputStream()) {
				shp = ShapefileZip.extrair(in, temp);
			}
			List<ShapefileTrechoFeature> features = ShapefileTrechoParser.ler(shp, atributoRua, atributoCodigo, charset);
			return persistirFeatures(
				empresa,
				setor,
				features,
				StatusTrecho.PLANEJADO,
				"TRECHO_IMPORTADO_SHAPEFILE",
				true
			);
		} catch (IOException exception) {
			throw new RegraNegocioException("Falha ao processar o ZIP do shapefile");
		} finally {
			apagarDiretorio(temp);
		}
	}

	@Transactional
	public TrechoImportacaoResponse criarDesenho(Long empresaId, TrechoDesenhoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());
		if (!setor.isAtivo()) {
			throw new RegraNegocioException("Setor informado está inativo");
		}
		if (request.geomWkts() == null || request.geomWkts().isEmpty()) {
			throw new RegraNegocioException("Desenhe ao menos um trecho no mapa");
		}
		if (request.geomWkts().size() > MAX_TRECHOS_DESENHO) {
			throw new RegraNegocioException("Limite de " + MAX_TRECHOS_DESENHO + " trechos por desenho");
		}

		String ruaNome = normalizarNomeRua(request.ruaNome());
		String codigoBase = request.codigo() == null || request.codigo().isBlank()
			? null
			: request.codigo().strip().toUpperCase(Locale.ROOT);
		List<ShapefileTrechoFeature> features = new ArrayList<>();
		int total = request.geomWkts().size();
		for (int indice = 0; indice < total; indice++) {
			LineString geom = GeometriaWkt.lineStringDeWkt(request.geomWkts().get(indice));
			String codigo = null;
			if (codigoBase != null) {
				codigo = total == 1 || indice == 0 ? codigoBase : codigoBase + "-" + (indice + 1);
			}
			features.add(new ShapefileTrechoFeature(indice + 1, codigo, ruaNome, geom));
		}

		StatusTrecho status = request.status() == null ? StatusTrecho.PLANEJADO : request.status();
		return persistirFeatures(empresa, setor, features, status, "TRECHO_CRIADO_DESENHO", false);
	}

	private TrechoImportacaoResponse persistirFeatures(
		Empresa empresa,
		Setor setor,
		List<ShapefileTrechoFeature> features,
		StatusTrecho status,
		String eventoAuditoria,
		boolean ignorarCodigoDuplicado
	) {
		Set<String> usados = new HashSet<>();
		for (Trecho existente : trechoRepository.findByEmpresaIdOrderByCodigoAsc(empresa.getId())) {
			usados.add(existente.getCodigo().toUpperCase(Locale.ROOT));
		}
		String prefixo = prefixoCodigo(setor);
		int sequencia = 1;
		int criados = 0;
		int ignorados = 0;
		List<TrechoImportacaoItemResponse> itens = new ArrayList<>();

		for (ShapefileTrechoFeature feature : features) {
			String codigo = codigoDaFeature(feature, prefixo, usados, sequencia);
			if (codigo == null) {
				if (!ignorarCodigoDuplicado) {
					throw new ConflitoNegocioException("Já existe trecho com o código " + feature.codigo());
				}
				ignorados++;
				itens.add(new TrechoImportacaoItemResponse(
					feature.indice(),
					feature.codigo(),
					feature.ruaNome(),
					"IGNORADO",
					"Código já cadastrado na empresa"
				));
				continue;
			}
			if (feature.codigo() == null || feature.codigo().isBlank()) {
				sequencia++;
			}
			usados.add(codigo);

			Trecho trecho = new Trecho();
			trecho.setEmpresa(empresa);
			trecho.setSetor(setor);
			trecho.setNomeRua(normalizarNomeRua(feature.ruaNome()));
			trecho.setCodigo(codigo);
			trecho.setGeom(feature.geom());
			trecho.setStatus(status);
			trecho.setPercentualExecutado(0f);
			trecho.setComprimentoMetros(GeometriaWkt.comprimentoMetros(feature.geom()));
			trecho.setAtivo(true);
			trechoRepository.save(trecho);
			criados++;
			itens.add(new TrechoImportacaoItemResponse(
				feature.indice(),
				codigo,
				trecho.getNomeRua(),
				"CRIADO",
				null
			));
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("setorId", setor.getId());
		payload.put("lidos", features.size());
		payload.put("criados", criados);
		payload.put("ignorados", ignorados);
		auditoriaService.registrar(empresa, eventoAuditoria, "Setor", setor.getId(), payload);
		LOGGER.info(
			"Trechos persistidos: evento={} empresaId={} setorId={} lidos={} criados={} ignorados={}",
			eventoAuditoria,
			empresa.getId(),
			setor.getId(),
			features.size(),
			criados,
			ignorados
		);
		return new TrechoImportacaoResponse(features.size(), criados, ignorados, itens);
	}

	private static String codigoDaFeature(
		ShapefileTrechoFeature feature,
		String prefixo,
		Set<String> usados,
		int sequencia
	) {
		if (feature.codigo() != null && !feature.codigo().isBlank()) {
			String codigo = feature.codigo().strip().toUpperCase(Locale.ROOT);
			if (codigo.length() > 80) {
				codigo = codigo.substring(0, 80);
			}
			return usados.contains(codigo) ? null : codigo;
		}
		String gerado;
		int tentativa = sequencia;
		do {
			gerado = prefixo + "-" + String.format(Locale.ROOT, "%04d", tentativa);
			tentativa++;
		} while (usados.contains(gerado));
		return gerado;
	}

	private static String prefixoCodigo(Setor setor) {
		if (setor.getCodigo() != null && !setor.getCodigo().isBlank()) {
			String codigo = setor.getCodigo().strip().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
			if (!codigo.isBlank()) {
				return codigo.length() > 20 ? codigo.substring(0, 20) : codigo;
			}
		}
		return "T" + setor.getId();
	}

	private static void apagarDiretorio(Path diretorio) {
		if (diretorio == null || !Files.exists(diretorio)) {
			return;
		}
		try (var stream = Files.walk(diretorio)) {
			stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
					// temp
				}
			});
		} catch (IOException ignored) {
			// temp
		}
	}

	@Transactional
	public TrechoResponse atualizar(Long empresaId, Long id, TrechoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Trecho trecho = buscarEntidade(empresaId, id);
		StatusTrecho statusAnterior = trecho.getStatus();
		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());
		if (!setor.isAtivo() && !setor.getId().equals(trecho.getSetor().getId())) {
			throw new RegraNegocioException("Setor informado está inativo");
		}
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, id);
		LineString geom = GeometriaWkt.lineStringDeWkt(request.geomWkt());

		trecho.setSetor(setor);
		trecho.setNomeRua(normalizarNomeRua(request.ruaNome()));
		trecho.setCodigo(codigo);
		trecho.setGeom(geom);
		aplicarCamposOperacionais(trecho, request, false);
		if (request.ativo() != null) {
			trecho.setAtivo(request.ativo());
		}
		trechoRepository.save(trecho);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("codigo", codigo);
		payload.put("statusAnterior", statusAnterior.name());
		payload.put("status", trecho.getStatus().name());
		auditoriaService.registrar(empresa, "TRECHO_ATUALIZADO", "Trecho", id, payload);
		if (statusAnterior != trecho.getStatus()) {
			LOGGER.info(
				"Transição de status do trecho id={}: {} -> {}",
				id,
				statusAnterior,
				trecho.getStatus()
			);
		}
		LOGGER.info("Trecho atualizado: id={}, empresaId={}", id, empresaId);
		return paraResponse(trecho);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Trecho trecho = buscarEntidade(empresaId, id);
		trecho.setAtivo(false);
		trechoRepository.save(trecho);
		auditoriaService.registrar(
			empresa,
			"TRECHO_DESATIVADO",
			"Trecho",
			id,
			Map.of("codigo", trecho.getCodigo())
		);
		LOGGER.info("Trecho desativado: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Trecho buscarEntidade(Long empresaId, Long id) {
		return trechoRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Trecho não encontrado"));
	}

	private void aplicarCamposOperacionais(Trecho trecho, TrechoRequest request, boolean criacao) {
		if (request.status() != null) {
			trecho.setStatus(request.status());
		} else if (criacao) {
			trecho.setStatus(StatusTrecho.PLANEJADO);
		}
		if (request.percentualExecutado() != null) {
			float pct = request.percentualExecutado();
			if (pct < 0f || pct > 100f) {
				throw new RegraNegocioException("percentualExecutado deve estar entre 0 e 100");
			}
			trecho.setPercentualExecutado(pct);
		}
		if (request.comprimentoMetros() != null) {
			trecho.setComprimentoMetros(Math.max(0f, request.comprimentoMetros()));
		} else if (criacao || request.geomWkt() != null) {
			trecho.setComprimentoMetros(GeometriaWkt.comprimentoMetros(trecho.getGeom()));
		}
		if (request.emCorrecao() != null) {
			trecho.setEmCorrecao(request.emCorrecao());
		}
		if (request.liberadoInforme() != null) {
			trecho.setLiberadoInforme(request.liberadoInforme());
		}
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
			? trechoRepository.existsByEmpresaIdAndCodigoIgnoreCase(empresaId, codigo)
			: trechoRepository.existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(empresaId, codigo, idAtual);
		if (existe) {
			throw new ConflitoNegocioException("Já existe trecho com este código na empresa");
		}
	}

	private String normalizarNomeRua(String ruaNome) {
		if (ruaNome == null || ruaNome.isBlank()) {
			return null;
		}
		return ruaNome.strip();
	}

	private TrechoResponse paraResponse(Trecho trecho) {
		Setor setor = trecho.getSetor();
		return new TrechoResponse(
			trecho.getId(),
			setor.getId(),
			setor.getNome(),
			setor.getCodigo(),
			trecho.getNomeRua() == null ? "" : trecho.getNomeRua(),
			trecho.getCodigo(),
			trecho.getStatus(),
			trecho.getPercentualExecutado(),
			trecho.getComprimentoMetros(),
			trecho.getUltimaVisitaEm(),
			trecho.isEmCorrecao(),
			trecho.isLiberadoInforme(),
			GeometriaWkt.paraWkt(trecho.getGeom()),
			trecho.isAtivo()
		);
	}
}
