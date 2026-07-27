package br.com.gop.models;

import java.time.Instant;
import java.time.LocalDate;

public record LicencaResponse(
	Long id,
	Long empresaId,
	String empresaCodigo,
	int tetoAtivos,
	LocalDate vigenciaInicio,
	LocalDate vigenciaFim,
	StatusLicenca status,
	boolean ativo,
	Instant criadoEm,
	Instant atualizadoEm
) {
}
