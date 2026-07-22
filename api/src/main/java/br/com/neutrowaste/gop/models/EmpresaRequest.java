package br.com.neutrowaste.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
	@NotBlank
	@Size(max = 50)
	@Pattern(regexp = "[A-Za-z0-9_-]+")
	String codigo,

	@NotBlank
	@Size(max = 200)
	String nome,

	Boolean ativo
) {
}
