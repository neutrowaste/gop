package br.com.gop.integration.rassystem;

import br.com.gop.models.TelemetriaGps;
import br.com.gop.services.IntegracaoExternaException;
import br.com.gop.services.RegraNegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP da API RasSystem (Basic Auth). Espelha os 6 GETs do legado
 * {@code apimonitoragps} e devolve JSON de cadastro ou {@link TelemetriaGps}.
 */
@Component
public class RasSystemClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(RasSystemClient.class);

	static final List<String> OP_RASTREIO = List.of(
		"velocidade", "direcao", "rd", "altitude", "odometro", "satelites"
	);
	static final List<String> OP_EVENTOS = List.of(
		"velocidade", "direcao", "rd", "altitude", "odometro", "satelites"
	);

	private final RestClient restClient;
	private final RasSystemProperties properties;

	public RasSystemClient(
		@Qualifier("rasSystemRestClient") RestClient restClient,
		RasSystemProperties properties
	) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public boolean isConfigurado() {
		return properties.isConfigurado();
	}

	public RasSystemProperties properties() {
		return properties;
	}

	public List<RasFilial> listarFiliais() {
		RasFilial[] corpo = get("/filiais", RasFilial[].class);
		return corpo == null ? List.of() : List.of(corpo);
	}

	public List<RasRotulo> listarRotulos(long idFilial) {
		RasRotulo[] corpo = get("/filiais/" + idFilial + "/rotulos", RasRotulo[].class);
		return corpo == null ? List.of() : List.of(corpo);
	}

	public List<RasVeiculo> listarVeiculos(long idRotulo) {
		RasVeiculo[] corpo = get("/rotulos/" + idRotulo + "/veiculos", RasVeiculo[].class);
		return corpo == null ? List.of() : List.of(corpo);
	}

	public List<TelemetriaGps> ultimasCoordenadas(Integer idFilial, Integer idRotulo, Integer idTipoServico) {
		int filial = exigirFilial(idFilial);
		String path;
		if (idRotulo != null) {
			path = "/veiculos/" + filial + "/rotulo/" + idRotulo + "/ultimasCoordenadas?completo=true";
		} else {
			int tipo = idTipoServico != null ? idTipoServico : 1;
			path = "/veiculos/" + filial + "/" + tipo + "/ultimasCoordenadas?completo=true";
		}
		return RasSystemGeoJsonParser.parseColecao(get(path, JsonNode.class));
	}

	public List<TelemetriaGps> rastreio(long idVeiculo, String dataInicio, String dataFim) {
		String path = "/veiculos/" + idVeiculo + "/rastreio"
			+ "?dataInicio=" + encodar(dataInicio)
			+ "&dataFim=" + encodar(dataFim)
			+ ops(OP_RASTREIO);
		return RasSystemGeoJsonParser.parseColecao(get(path, JsonNode.class));
	}

	public List<TelemetriaGps> eventos(long idVeiculo, String dataInicio, String dataFim) {
		String path = "/veiculos/" + idVeiculo + "/eventos"
			+ "?dataInicio=" + encodar(dataInicio)
			+ "&dataFim=" + encodar(dataFim)
			+ ops(OP_EVENTOS);
		return RasSystemGeoJsonParser.parseEventos(get(path, JsonNode.class));
	}

	private int exigirFilial(Integer idFilial) {
		Integer filial = idFilial != null ? idFilial : properties.getIdFilial();
		if (filial == null) {
			throw new RegraNegocioException("Informe idFilial ou configure gop.rassystem.id-filial");
		}
		return filial;
	}

	private void garantirConfigurado() {
		if (!properties.isConfigurado()) {
			throw new RegraNegocioException(
				"Integração RasSystem não configurada. Defina RASSYSTEM_LOGIN e RASSYSTEM_SENHA."
			);
		}
	}

	private <T> T get(String path, Class<T> tipo) {
		garantirConfigurado();
		try {
			return restClient.get()
				.uri(path)
				.headers(this::autorizar)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
					int status = res.getStatusCode().value();
					String recorte = body.isBlank() ? "" : ": " + body.substring(0, Math.min(200, body.length()));
					LOGGER.warn("RasSystem HTTP {} em {} (login={}){}", status, path, properties.getLogin(), recorte);
					if (status == 401) {
						throw new IntegracaoExternaException(
							"RasSystem recusou o login \"" + properties.getLogin()
								+ "\" (HTTP 401). Confira gop.rassystem.login/senha "
								+ "ou as variáveis RASSYSTEM_LOGIN e RASSYSTEM_SENHA (no legado era RASSYSTEM_PASSWORD).",
							status
						);
					}
					throw new IntegracaoExternaException(
						"RasSystem respondeu HTTP " + status + " em " + path + recorte,
						status
					);
				})
				.body(tipo);
		} catch (IntegracaoExternaException | RegraNegocioException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new IntegracaoExternaException(
				"Falha ao consultar RasSystem em " + path + ": " + exception.getMessage(),
				exception
			);
		}
	}

	private void autorizar(HttpHeaders headers) {
		headers.setBasicAuth(properties.getLogin(), properties.getSenha(), StandardCharsets.UTF_8);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	}

	private static String ops(List<String> campos) {
		StringBuilder query = new StringBuilder();
		for (String campo : campos) {
			query.append("&op=").append(campo);
		}
		return query.toString();
	}

	private static String encodar(String valor) {
		return java.net.URLEncoder.encode(valor, StandardCharsets.UTF_8);
	}
}
