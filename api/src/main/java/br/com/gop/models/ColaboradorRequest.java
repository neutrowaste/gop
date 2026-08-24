package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ColaboradorRequest(
	@NotBlank @Size(max = 200) String nome,
	@NotBlank @Size(max = 60) String matricula,
	@NotNull Long equipamentoId,
	Boolean ativo
) {
}
