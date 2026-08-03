package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SetorRequest(
	@NotNull Long cidadeId,
	@Size(max = 40) String codigo,
	@NotBlank @Size(max = 200) String nome,
	Boolean ativo
) {
}
