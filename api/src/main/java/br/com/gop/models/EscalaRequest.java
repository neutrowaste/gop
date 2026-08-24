package br.com.gop.models;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record EscalaRequest(
	TipoEscala tipo,
	LocalDate data,
	List<DayOfWeek> diasSemana,
	@NotNull Turno turno,
	@NotNull Long setorId,
	@NotEmpty List<@NotNull Long> colaboradorIds,
	@NotNull Long supervisorUsuarioId,
	List<@NotNull Long> trechoIds,
	Boolean ativo
) {
}
