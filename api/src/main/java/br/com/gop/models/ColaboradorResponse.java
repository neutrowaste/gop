package br.com.gop.models;

public record ColaboradorResponse(
	Long id,
	Long equipeId,
	String equipeNome,
	String nome,
	String matricula,
	boolean ativo
) {
}
