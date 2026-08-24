package br.com.gop.models;

public record EquipamentoResponse(
	Long id,
	String codigo,
	String descricao,
	String codigoExterno,
	FonteGps fonte,
	boolean ativo
) {
}
