package br.com.gop.models;

public record EquipeResponse(
	Long id,
	Long setorId,
	String setorNome,
	String nome,
	boolean ativo
) {
}
