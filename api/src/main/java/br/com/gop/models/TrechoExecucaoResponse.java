package br.com.gop.models;

import java.time.Instant;

public record TrechoExecucaoResponse(
	Long id,
	Long escalaId,
	Long trechoId,
	String trechoCodigo,
	String ruaNome,
	int ordem,
	StatusTrecho status,
	float percentualExecutado,
	int quantidadePosicoesValidas,
	boolean tevePosicaoInicio,
	boolean tevePosicaoMeio,
	boolean tevePosicaoFim,
	Instant primeiraPosicaoValidaEm,
	Instant ultimaPosicaoValidaEm,
	boolean ativo
) {
}
