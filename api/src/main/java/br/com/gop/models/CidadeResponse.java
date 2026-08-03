package br.com.gop.models;

public record CidadeResponse(
	Long id,
	String nome,
	String uf,
	boolean ativo
) {
}
