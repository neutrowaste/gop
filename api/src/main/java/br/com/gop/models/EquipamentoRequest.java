package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipamentoRequest(
	@NotNull Long equipeId,
	@NotBlank @Size(max = 60) String codigo,
	@Size(max = 200) String descricao,
	Boolean ativo
) {
}
