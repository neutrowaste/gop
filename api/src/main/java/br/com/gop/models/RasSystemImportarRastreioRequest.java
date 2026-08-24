package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;

public record RasSystemImportarRastreioRequest(
	@NotBlank String veiculo,
	@NotBlank String dataInicio,
	@NotBlank String dataFim,
	Integer idFilial,
	Boolean persistir
) {
}
