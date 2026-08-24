package br.com.gop.integration.rassystem;

import br.com.gop.models.FonteGps;
import br.com.gop.models.TelemetriaGps;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converte FeatureCollection GeoJSON da RasSystem no contrato interno
 * {@link TelemetriaGps}. Números podem vir como number ou string.
 */
public final class RasSystemGeoJsonParser {

	private static final Set<String> CAMPOS_CONHECIDOS = Set.of(
		"idVeiculo",
		"veiculo",
		"data",
		"velocidade",
		"direcao",
		"altitude",
		"odometro",
		"satelites",
		"rd",
		"situacao",
		"tipoServico",
		"modelo",
		"setor",
		"rotulos",
		"tipo",
		"valor"
	);

	private RasSystemGeoJsonParser() {
	}

	public static List<TelemetriaGps> parseColecao(JsonNode raiz) {
		List<TelemetriaGps> resultado = new ArrayList<>();
		if (raiz == null) {
			return resultado;
		}
		JsonNode features = raiz.get("features");
		if (features == null || !features.isArray()) {
			return resultado;
		}
		for (JsonNode feature : features) {
			TelemetriaGps ponto = parseFeature(feature);
			if (ponto != null) {
				resultado.add(ponto);
			}
		}
		return resultado;
	}

	public static List<TelemetriaGps> parseEventos(JsonNode raiz) {
		List<TelemetriaGps> resultado = new ArrayList<>();
		if (raiz == null || raiz.get("features") == null || !raiz.get("features").isArray()) {
			return resultado;
		}
		for (JsonNode feature : raiz.get("features")) {
			TelemetriaGps ponto = parseFeature(feature);
			if (ponto == null) {
				continue;
			}
			if (ponto.tipoEvento() == null || ponto.tipoEvento().isBlank()) {
				continue;
			}
			resultado.add(ponto);
		}
		return resultado;
	}

	static TelemetriaGps parseFeature(JsonNode feature) {
		if (feature == null) {
			return null;
		}
		JsonNode geometry = feature.get("geometry");
		JsonNode coordinates = geometry != null ? geometry.get("coordinates") : null;
		if (coordinates == null || !coordinates.isArray() || coordinates.size() < 2) {
			return null;
		}
		Double longitude = numero(coordinates.get(0));
		Double latitude = numero(coordinates.get(1));
		if (longitude == null || latitude == null) {
			return null;
		}

		JsonNode props = feature.get("properties");
		if (props == null || !props.isObject()) {
			return null;
		}
		Long idVeiculo = inteiroLongo(props.get("idVeiculo"));
		if (idVeiculo == null) {
			return null;
		}

		String rotuloVeiculo = texto(props.get("veiculo"));
		return new TelemetriaGps(
			FonteGps.RASSYSTEM,
			idVeiculo,
			RasSystemLutocarMatcher.codigoLogico(rotuloVeiculo),
			rotuloVeiculo,
			latitude,
			longitude,
			RasSystemInstanteParser.parseOuAgora(texto(props.get("data"))),
			numeroFloat(props.get("velocidade")),
			numeroFloat(props.get("direcao")),
			numeroFloat(props.get("altitude")),
			inteiroLongo(props.get("odometro")),
			inteiro(props.get("satelites")),
			inteiroLongo(props.get("rd")),
			texto(props.get("situacao")),
			inteiro(props.get("tipoServico")),
			inteiro(props.get("modelo")),
			texto(props.get("setor")),
			inteiros(props.get("rotulos")),
			texto(props.get("tipo")),
			texto(props.get("valor")),
			propriedades(props)
		);
	}

	private static Map<String, Object> propriedades(JsonNode props) {
		Map<String, Object> extras = new LinkedHashMap<>();
		Iterator<Map.Entry<String, JsonNode>> campos = props.fields();
		while (campos.hasNext()) {
			Map.Entry<String, JsonNode> campo = campos.next();
			if (CAMPOS_CONHECIDOS.contains(campo.getKey())) {
				continue;
			}
			JsonNode valor = campo.getValue();
			if (valor == null || valor.isNull()) {
				continue;
			}
			if (valor.isNumber()) {
				extras.put(campo.getKey(), valor.numberValue());
			} else if (valor.isBoolean()) {
				extras.put(campo.getKey(), valor.booleanValue());
			} else if (valor.isTextual()) {
				extras.put(campo.getKey(), valor.asText());
			} else if (valor.isArray()) {
				List<Object> itens = new ArrayList<>();
				for (JsonNode item : valor) {
					if (item.isNumber()) {
						itens.add(item.numberValue());
					} else if (item.isTextual()) {
						itens.add(item.asText());
					}
				}
				extras.put(campo.getKey(), itens);
			}
		}
		return extras.isEmpty() ? Map.of() : extras;
	}

	private static List<Integer> inteiros(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		List<Integer> valores = new ArrayList<>();
		for (JsonNode item : node) {
			Integer valor = inteiro(item);
			if (valor != null) {
				valores.add(valor);
			}
		}
		return List.copyOf(valores);
	}

	static Double numero(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		if (node.isNumber()) {
			double valor = node.asDouble();
			return Double.isFinite(valor) ? valor : null;
		}
		if (node.isTextual()) {
			String texto = node.asText().strip();
			if (texto.isEmpty() || "null".equalsIgnoreCase(texto)) {
				return null;
			}
			try {
				double valor = Double.parseDouble(texto);
				return Double.isFinite(valor) ? valor : null;
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	static Float numeroFloat(JsonNode node) {
		Double valor = numero(node);
		return valor == null ? null : valor.floatValue();
	}

	static Long inteiroLongo(JsonNode node) {
		Double valor = numero(node);
		if (valor == null) {
			return null;
		}
		return valor.longValue();
	}

	static Integer inteiro(JsonNode node) {
		Long valor = inteiroLongo(node);
		return valor == null ? null : valor.intValue();
	}

	static String texto(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		if (node.isTextual()) {
			String valor = node.asText().strip();
			return valor.isEmpty() ? null : valor;
		}
		if (node.isNumber() || node.isBoolean()) {
			return node.asText();
		}
		return null;
	}
}
