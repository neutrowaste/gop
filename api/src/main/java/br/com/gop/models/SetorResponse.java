package br.com.gop.models;

public record SetorResponse(
	Long id,
	Long cidadeId,
	String cidadeNome,
	String codigo,
	String nome,
	boolean ativo
) {
}
