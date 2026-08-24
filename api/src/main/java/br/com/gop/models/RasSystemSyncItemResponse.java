package br.com.gop.models;

public record RasSystemSyncItemResponse(
	Long idVeiculo,
	String codigoLogico,
	String rotuloVeiculo,
	String status,
	Long equipamentoId,
	String detalhe
) {
}
