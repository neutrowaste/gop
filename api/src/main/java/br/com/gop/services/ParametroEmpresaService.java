package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.ParametroEmpresa;
import br.com.gop.models.ParametroEmpresaRequest;
import br.com.gop.models.ParametroEmpresaResponse;
import br.com.gop.repositories.ParametroEmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Limiares do motor de Varrição (RN-001), configuráveis por empresa em vez de
 * hardcode. Quando a empresa não configurou uma chave, o valor padrão do RN-001
 * é usado (ver {@link #DESCRICOES} e os métodos tipados abaixo).
 */
@Service
@Profile("postgres")
public class ParametroEmpresaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParametroEmpresaService.class);

	public static final String COBERTURA_MINIMA_PCT = "cobertura_minima_pct";
	public static final String SNAP_BUFFER_METROS = "snap_buffer_metros";
	public static final String SILENCIO_GPS_MINUTOS = "silencio_gps_minutos";
	public static final String TOLERANCIA_SINAL_MINUTOS = "tolerancia_sinal_minutos";
	public static final String VELOCIDADE_MAXIMA_KMH = "velocidade_maxima_kmh";

	private static final Map<String, Double> DEFAULTS = Map.of(
		COBERTURA_MINIMA_PCT, 80d,
		SNAP_BUFFER_METROS, 25d,
		SILENCIO_GPS_MINUTOS, 60d,
		TOLERANCIA_SINAL_MINUTOS, 30d,
		VELOCIDADE_MAXIMA_KMH, 5d
	);

	private static final Map<String, String> DESCRICOES = Map.of(
		COBERTURA_MINIMA_PCT, "Percentual mínimo (0-100) do trecho para ser considerado EXECUTADO",
		SNAP_BUFFER_METROS, "Distância máxima (metros) do eixo do trecho para uma posição ser válida",
		SILENCIO_GPS_MINUTOS, "Minutos sem transmissão do GPS para caracterizar falha (RN-001: >= 60)",
		TOLERANCIA_SINAL_MINUTOS, "Minutos de tolerância de perda de sinal antes do silêncio contar",
		VELOCIDADE_MAXIMA_KMH, "Velocidade máxima (km/h) considerada compatível com varrição"
	);

	public static final List<String> CHAVES_CONHECIDAS = List.copyOf(DEFAULTS.keySet());

	private final EmpresaService empresaService;
	private final ParametroEmpresaRepository parametroEmpresaRepository;
	private final AuditoriaService auditoriaService;

	public ParametroEmpresaService(
		EmpresaService empresaService,
		ParametroEmpresaRepository parametroEmpresaRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.parametroEmpresaRepository = parametroEmpresaRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<ParametroEmpresaResponse> listar(Long empresaId) {
		empresaService.buscarEntidade(empresaId);
		Map<String, ParametroEmpresa> configurados = new LinkedHashMap<>();
		for (ParametroEmpresa parametro : parametroEmpresaRepository.findByEmpresaIdOrderByChaveAsc(empresaId)) {
			configurados.put(parametro.getChave(), parametro);
		}
		return CHAVES_CONHECIDAS.stream()
			.map(chave -> {
				ParametroEmpresa existente = configurados.get(chave);
				if (existente != null && existente.isAtivo()) {
					return new ParametroEmpresaResponse(
						chave,
						existente.getValor(),
						existente.getTipo(),
						existente.getDescricao(),
						false,
						true
					);
				}
				return new ParametroEmpresaResponse(
					chave,
					String.valueOf(DEFAULTS.get(chave)),
					"NUMERO",
					DESCRICOES.get(chave),
					true,
					true
				);
			})
			.toList();
	}

	@Transactional
	public ParametroEmpresaResponse definir(Long empresaId, String chave, ParametroEmpresaRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		String chaveNormalizada = validarChaveConhecida(chave);
		validarValorNumerico(chaveNormalizada, request.valor());

		ParametroEmpresa parametro = parametroEmpresaRepository
			.findByEmpresaIdAndChave(empresaId, chaveNormalizada)
			.orElseGet(() -> {
				ParametroEmpresa novo = new ParametroEmpresa();
				novo.setEmpresa(empresa);
				novo.setChave(chaveNormalizada);
				return novo;
			});
		parametro.setValor(request.valor().strip());
		parametro.setTipo(request.tipo() == null || request.tipo().isBlank() ? "NUMERO" : request.tipo());
		parametro.setDescricao(request.descricao());
		parametro.setAtivo(true);
		parametroEmpresaRepository.save(parametro);

		auditoriaService.registrar(
			empresa,
			"PARAMETRO_EMPRESA_DEFINIDO",
			"ParametroEmpresa",
			parametro.getId(),
			Map.of("chave", chaveNormalizada, "valor", parametro.getValor())
		);
		LOGGER.info("Parâmetro definido: empresaId={}, chave={}, valor={}", empresaId, chaveNormalizada, parametro.getValor());
		return new ParametroEmpresaResponse(
			chaveNormalizada,
			parametro.getValor(),
			parametro.getTipo(),
			parametro.getDescricao(),
			false,
			true
		);
	}

	@Transactional
	public void restaurarPadrao(Long empresaId, String chave) {
		Empresa empresa = empresaAtiva(empresaId);
		String chaveNormalizada = validarChaveConhecida(chave);
		parametroEmpresaRepository.findByEmpresaIdAndChaveAndAtivoTrue(empresaId, chaveNormalizada)
			.ifPresent(parametro -> {
				parametro.setAtivo(false);
				parametroEmpresaRepository.save(parametro);
				auditoriaService.registrar(
					empresa,
					"PARAMETRO_EMPRESA_RESTAURADO",
					"ParametroEmpresa",
					parametro.getId(),
					Map.of("chave", chaveNormalizada)
				);
				LOGGER.info("Parâmetro restaurado ao padrão: empresaId={}, chave={}", empresaId, chaveNormalizada);
			});
	}

	@Transactional(readOnly = true)
	public double coberturaMinimaPercentual(Long empresaId) {
		return obterNumero(empresaId, COBERTURA_MINIMA_PCT);
	}

	@Transactional(readOnly = true)
	public double snapBufferMetros(Long empresaId) {
		return obterNumero(empresaId, SNAP_BUFFER_METROS);
	}

	@Transactional(readOnly = true)
	public long silencioGpsMinutos(Long empresaId) {
		return (long) obterNumero(empresaId, SILENCIO_GPS_MINUTOS);
	}

	@Transactional(readOnly = true)
	public long toleranciaSinalMinutos(Long empresaId) {
		return (long) obterNumero(empresaId, TOLERANCIA_SINAL_MINUTOS);
	}

	@Transactional(readOnly = true)
	public double velocidadeMaximaKmh(Long empresaId) {
		return obterNumero(empresaId, VELOCIDADE_MAXIMA_KMH);
	}

	private double obterNumero(Long empresaId, String chave) {
		return parametroEmpresaRepository.findByEmpresaIdAndChaveAndAtivoTrue(empresaId, chave)
			.map(ParametroEmpresa::getValor)
			.map(this::converterNumero)
			.orElseGet(() -> DEFAULTS.get(chave));
	}

	private double converterNumero(String valor) {
		try {
			return Double.parseDouble(valor.replace(',', '.'));
		} catch (NumberFormatException exception) {
			throw new RegraNegocioException("Valor de parâmetro inválido: " + valor);
		}
	}

	private void validarValorNumerico(String chave, String valor) {
		converterNumero(valor);
	}

	private String validarChaveConhecida(String chave) {
		String normalizada = chave == null ? "" : chave.strip().toLowerCase(java.util.Locale.ROOT);
		if (!CHAVES_CONHECIDAS.contains(normalizada)) {
			throw new RegraNegocioException("Parâmetro desconhecido: " + chave);
		}
		return normalizada;
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}
}
