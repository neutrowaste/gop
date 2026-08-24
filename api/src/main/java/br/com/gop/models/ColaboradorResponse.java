package br.com.gop.models;

public record ColaboradorResponse(
	Long id,
	String nome,
	String matricula,
	Long equipamentoId,
	String equipamentoCodigo,
	boolean ativo
) {
}
