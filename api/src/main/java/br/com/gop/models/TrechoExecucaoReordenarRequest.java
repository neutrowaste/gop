package br.com.gop.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TrechoExecucaoReordenarRequest(
	@NotNull @Min(1) Integer ordem
) {
}
