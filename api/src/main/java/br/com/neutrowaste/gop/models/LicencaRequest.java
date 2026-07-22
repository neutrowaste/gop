package br.com.neutrowaste.gop.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LicencaRequest(
	@Min(0) int tetoAtivos,
	@NotNull LocalDate vigenciaInicio,
	LocalDate vigenciaFim,
	@NotNull StatusLicenca status,
	Boolean ativo
) {
}
