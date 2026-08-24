package br.com.gop.integration.rassystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RasRotulo(
	Long id,
	String nome,
	Boolean interno
) {
}
