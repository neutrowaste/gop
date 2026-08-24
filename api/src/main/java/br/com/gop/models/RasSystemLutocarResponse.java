package br.com.gop.models;

public record RasSystemLutocarResponse(
	Long idVeiculo,
	String descricao,
	String placa,
	String codigoLogico,
	Long equipamentoId,
	String equipamentoCodigo,
	boolean vinculado,
	TelemetriaGps ultimaPosicao
) {
}
