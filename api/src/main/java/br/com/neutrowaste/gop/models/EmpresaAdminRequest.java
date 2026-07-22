package br.com.neutrowaste.gop.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmpresaAdminRequest(
	@NotBlank
	@Email
	@Size(max = 200)
	String email,

	@NotBlank
	@Size(min = 8, max = 100)
	String senha,

	@NotBlank
	@Size(max = 200)
	String nome
) {
}
