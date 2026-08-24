package br.com.gop.models;

public record TrechoImportacaoItemResponse(
	int indice,
	String codigo,
	String ruaNome,
	String status,
	String detalhe
) {
}
