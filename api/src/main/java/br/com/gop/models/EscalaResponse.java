package br.com.gop.models;

import java.time.Instant;
import java.time.LocalDate;

public record EscalaResponse(
	Long id,
	LocalDate data,
	Turno turno,
	Long setorId,
	String setorNome,
	Long equipeId,
	String equipeNome,
	Long equipamentoId,
	String equipamentoCodigo,
	Long supervisorUsuarioId,
	String supervisorNome,
	StatusEscala status,
	Instant publicadaEm,
	boolean ativo
) {
}
