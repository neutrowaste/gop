package br.com.gop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "gop.cors")
public class CorsProperties {

	private String origins = "http://localhost:4200";
	private String originPatterns = "";

	public String getOrigins() {
		return origins;
	}

	public void setOrigins(String origins) {
		this.origins = origins;
	}

	public String getOriginPatterns() {
		return originPatterns;
	}

	public void setOriginPatterns(String originPatterns) {
		this.originPatterns = originPatterns;
	}

	public List<String> origensPermitidas() {
		return partir(origins, List.of("http://localhost:4200"));
	}

	public List<String> padroesOrigem() {
		return partir(originPatterns, List.of());
	}

	private static List<String> partir(String valor, List<String> padrao) {
		if (valor == null || valor.isBlank()) {
			return padrao;
		}
		List<String> itens = Arrays.stream(valor.split(","))
			.map(String::trim)
			.filter(item -> !item.isEmpty())
			.toList();
		return itens.isEmpty() ? padrao : itens;
	}
}
