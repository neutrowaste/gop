package br.com.gop.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TrechoExecucaoRequest(
	@NotNull Long trechoId,
	@NotNull @Min(1) Integer ordem
) {
}
