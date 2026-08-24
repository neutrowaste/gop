package br.com.gop.models;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EscalaResponse(
	Long id,
	TipoEscala tipo,
	LocalDate data,
	List<DayOfWeek> diasSemana,
	Long origemId,
	Turno turno,
	Long setorId,
	String setorNome,
	List<EscalaColaboradorResponse> colaboradores,
	Long supervisorUsuarioId,
	String supervisorNome,
	StatusEscala status,
	Instant publicadaEm,
	boolean ativo
) {
}
