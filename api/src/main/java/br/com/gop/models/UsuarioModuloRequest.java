package br.com.gop.models;

import jakarta.validation.constraints.NotNull;

public record UsuarioModuloRequest(
	@NotNull Boolean liberado
) {
}
