package br.com.gop.models;

public record SetorResponse(
	Long id,
	Long moduloId,
	String moduloCodigo,
	String moduloNome,
	Long cidadeId,
	String cidadeNome,
	String codigo,
	String nome,
	boolean temDemarcacao,
	String geomWkt,
	GeoJsonGeometry geomGeoJson,
	boolean ativo
) {
}
