package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrechoRequest(
	@NotNull Long setorId,
	@Size(max = 200) String ruaNome,
	@NotBlank @Size(max = 80) String codigo,
	@NotBlank String geomWkt,
	StatusTrecho status,
	Float percentualExecutado,
	Float comprimentoMetros,
	Boolean emCorrecao,
	Boolean liberadoInforme,
	Boolean ativo
) {
}
