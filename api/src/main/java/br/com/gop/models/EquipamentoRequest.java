package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipamentoRequest(
	@NotBlank @Size(max = 60) String codigo,
	@Size(max = 200) String descricao,
	@Size(max = 80) String codigoExterno,
	Boolean ativo,
	FonteGps fonte
) {
}
