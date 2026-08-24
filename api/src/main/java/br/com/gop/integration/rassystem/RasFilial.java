package br.com.gop.integration.rassystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RasFilial(
	Long idFilial,
	String nome,
	String empresa
) {
}
