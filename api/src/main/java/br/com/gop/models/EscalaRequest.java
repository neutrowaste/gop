package br.com.gop.models;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EscalaRequest(
	@NotNull LocalDate data,
	@NotNull Turno turno,
	@NotNull Long setorId,
	@NotNull Long equipeId,
	@NotNull Long equipamentoId,
	@NotNull Long supervisorUsuarioId,
	Boolean ativo
) {
}
