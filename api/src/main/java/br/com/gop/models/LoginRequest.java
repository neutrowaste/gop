package br.com.gop.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank @Email String email,
	@NotBlank String senha
) {
}
