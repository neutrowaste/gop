package br.com.gop.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrechoDesenhoRequest(
	@NotNull Long setorId,
	@Size(max = 200) String ruaNome,
	@Size(max = 80) String codigo,
	@NotEmpty List<@NotBlank String> geomWkts,
	StatusTrecho status
) {
}
