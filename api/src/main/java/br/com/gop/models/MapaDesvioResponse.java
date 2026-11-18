package br.com.gop.models;

/**
 * Trecho do trajeto real em que o GPS saiu do eixo do plano do dia.
 * A faixa verde sobre a rua prevista continua vindo do percentual do trecho;
 * esta linha só existe quando há posições fora do buffer de snap.
 */
public record MapaDesvioResponse(
	Long escalaId,
	Long equipamentoId,
	String equipamentoCodigo,
	String colaboradoresNomes,
	Long setorId,
	String setorNome,
	GeoJsonGeometry geometry
) {
}
