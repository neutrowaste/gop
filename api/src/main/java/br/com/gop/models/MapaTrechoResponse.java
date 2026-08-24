package br.com.gop.models;

/**
 * Trecho do plano do dia (Escala PUBLICADA) com geometria e status de
 * execução, para exibição no mapa operacional (RN-001).
 */
public record MapaTrechoResponse(
	Long trechoExecucaoId,
	Long escalaId,
	Long trechoId,
	String trechoCodigo,
	String ruaNome,
	Long setorId,
	String setorNome,
	String colaboradoresNomes,
	int ordem,
	StatusTrecho status,
	float percentualExecutado,
	GeoJsonGeometry geometry
) {
}
