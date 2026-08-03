package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Rua;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoRequest;
import br.com.gop.models.TrechoResponse;
import br.com.gop.repositories.TrechoRepository;
import org.locationtech.jts.geom.LineString;
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
public class TrechoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TrechoService.class);

	private final EmpresaService empresaService;
	private final RuaService ruaService;
	private final TrechoRepository trechoRepository;
	private final AuditoriaService auditoriaService;

	public TrechoService(
		EmpresaService empresaService,
		RuaService ruaService,
		TrechoRepository trechoRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.ruaService = ruaService;
		this.trechoRepository = trechoRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<TrechoResponse> listar(Long empresaId, Long ruaId) {
		empresaService.buscarEntidade(empresaId);
		List<Trecho> trechos = ruaId == null
			? trechoRepository.findByEmpresaIdOrderByCodigoAsc(empresaId)
			: trechoRepository.findByEmpresaIdAndRuaIdOrderByCodigoAsc(empresaId, ruaId);
		return trechos.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public TrechoResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public TrechoResponse criar(Long empresaId, TrechoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Rua rua = ruaService.buscarEntidade(empresaId, request.ruaId());
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, null);
		LineString geom = GeometriaWkt.lineStringDeWkt(request.geomWkt());

		Trecho trecho = new Trecho();
		trecho.setEmpresa(empresa);
		trecho.setRua(rua);
		trecho.setCodigo(codigo);
		trecho.setGeom(geom);
		aplicarCamposOperacionais(trecho, request, true);
		trecho.setAtivo(request.ativo() == null || request.ativo());
		trechoRepository.save(trecho);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("codigo", codigo);
		payload.put("ruaId", rua.getId());
		payload.put("status", trecho.getStatus().name());
		auditoriaService.registrar(empresa, "TRECHO_CRIADO", "Trecho", trecho.getId(), payload);
		LOGGER.info("Trecho criado: id={}, empresaId={}, codigo={}", trecho.getId(), empresaId, codigo);
		return paraResponse(trecho);
	}

	@Transactional
	public TrechoResponse atualizar(Long empresaId, Long id, TrechoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Trecho trecho = buscarEntidade(empresaId, id);
		StatusTrecho statusAnterior = trecho.getStatus();
		Rua rua = ruaService.buscarEntidade(empresaId, request.ruaId());
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, id);
		LineString geom = GeometriaWkt.lineStringDeWkt(request.geomWkt());

		trecho.setRua(rua);
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

	private TrechoResponse paraResponse(Trecho trecho) {
		return new TrechoResponse(
			trecho.getId(),
			trecho.getRua().getId(),
			trecho.getRua().getNome(),
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
