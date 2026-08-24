package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;

public record SetorDemarcacaoRequest(
	@NotBlank String geomWkt
) {
}
