package br.com.gop.models;

public record RasSystemImportarCadastroItemResponse(
	Long idVeiculo,
	String codigo,
	String status,
	Long equipamentoId,
	String detalhe
) {
}
