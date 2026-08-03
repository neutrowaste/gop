package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RuaRequest(
	@NotNull Long cidadeId,
	@NotBlank @Size(max = 200) String nome,
	@NotBlank String geomWkt,
	Boolean ativo
) {
}
