package br.com.gop.models;

public record EquipamentoResponse(
	Long id,
	Long equipeId,
	String equipeNome,
	String codigo,
	String descricao,
	boolean ativo
) {
}
