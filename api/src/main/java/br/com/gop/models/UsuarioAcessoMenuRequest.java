package br.com.gop.models;

import jakarta.validation.constraints.NotNull;

public record UsuarioAcessoMenuRequest(
	@NotNull Boolean liberado
) {
}
