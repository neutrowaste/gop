package br.com.gop.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OcorrenciaResolucaoRequest(
	@NotNull StatusOcorrencia status,
	@Size(max = 500) String observacao
) {
}
