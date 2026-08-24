package br.com.gop.models;

public record EscalaColaboradorResponse(
	Long id,
	String nome,
	String matricula,
	Long equipamentoId,
	String equipamentoCodigo
) {
}
