package br.com.gop.models;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record PosicaoGpsRequest(
	Long equipamentoId,
	@Size(max = 60) String equipamentoCodigo,
	@Size(max = 80) String codigoExterno,
	@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
	@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
	Float velocidadeKmh,
	Float precisaoMetros,
	Instant capturadoEm,
	Float direcaoGraus,
	Float altitudeMetros,
	Long odometro,
	Integer satelites,
	Long rd,
	@Size(max = 40) String situacao,
	FonteGps fonte
) {
}
