package br.com.gop.models;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PosicaoGpsRequest(
	@NotNull Long equipamentoId,
	@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
	@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
	Float velocidadeKmh,
	Float precisaoMetros,
	Instant capturadoEm
) {
}
