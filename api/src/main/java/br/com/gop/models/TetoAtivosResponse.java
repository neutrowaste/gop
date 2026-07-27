package br.com.gop.models;

public record TetoAtivosResponse(
	Long empresaId,
	Long licencaId,
	int tetoAtivos,
	long ativosEmUso,
	long saldoDisponivel,
	boolean permiteNovoAtivo
) {
}
