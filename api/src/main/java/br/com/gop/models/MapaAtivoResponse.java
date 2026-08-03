package br.com.gop.models;

import java.time.Instant;

/**
 * Posição atual (última recebida) de um ativo GPS em escala PUBLICADA do dia,
 * para exibição no mapa operacional (RN-001).
 */
public record MapaAtivoResponse(
	Long escalaId,
	Long equipamentoId,
	String equipamentoCodigo,
	Long equipeId,
	String equipeNome,
	Long setorId,
	String setorNome,
	Turno turno,
	Long supervisorUsuarioId,
	String supervisorNome,
	double latitude,
	double longitude,
	Float velocidadeKmh,
	Instant recebidoEm,
	SituacaoSinal situacaoSinal
) {
}
