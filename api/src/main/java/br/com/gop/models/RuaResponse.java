package br.com.gop.models;

public record RuaResponse(
	Long id,
	Long cidadeId,
	String cidadeNome,
	String nome,
	String geomWkt,
	boolean ativo
) {
}
