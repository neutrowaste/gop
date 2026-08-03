package br.com.gop.models;

import java.time.Instant;

public record PosicaoGpsResponse(
	Long id,
	Long equipamentoId,
	String equipamentoCodigo,
	double latitude,
	double longitude,
	Float velocidadeKmh,
	Float precisaoMetros,
	Instant capturadoEm,
	Instant recebidoEm,
	Long trechoExecucaoId,
	String trechoCodigo,
	Float distanciaEixoMetros,
	Float progressoLinear,
	boolean valido,
	boolean escalaEncontrada
) {
}
