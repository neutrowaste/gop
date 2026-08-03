package br.com.gop.models;

public record ParametroEmpresaResponse(
	String chave,
	String valor,
	String tipo,
	String descricao,
	boolean padrao,
	boolean ativo
) {
}
