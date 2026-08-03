package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CidadeRequest(
	@NotBlank @Size(max = 120) String nome,
	@Size(min = 2, max = 2) String uf,
	Boolean ativo
) {
}
