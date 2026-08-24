package br.com.gop.integration.rassystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RasVeiculo(
	Long id,
	String descricao,
	String placa
) {
}
