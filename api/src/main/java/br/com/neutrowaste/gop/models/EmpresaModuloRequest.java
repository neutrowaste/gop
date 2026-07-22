package br.com.neutrowaste.gop.models;

import jakarta.validation.constraints.NotNull;

public record EmpresaModuloRequest(
	@NotNull Boolean habilitado
) {
}
