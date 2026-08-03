package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParametroEmpresaRequest(
	@NotBlank @Size(max = 500) String valor,
	@Size(max = 30) String tipo,
	@Size(max = 255) String descricao
) {
}
