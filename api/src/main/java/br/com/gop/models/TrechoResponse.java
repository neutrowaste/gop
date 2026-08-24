package br.com.gop.models;

import java.time.Instant;

public record TrechoResponse(
	Long id,
	Long setorId,
	String setorNome,
	String setorCodigo,
	String ruaNome,
	String codigo,
	StatusTrecho status,
	float percentualExecutado,
	float comprimentoMetros,
	Instant ultimaVisitaEm,
	boolean emCorrecao,
	boolean liberadoInforme,
	String geomWkt,
	boolean ativo
) {
}
