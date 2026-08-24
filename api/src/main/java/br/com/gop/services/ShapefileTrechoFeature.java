package br.com.gop.services;

import org.locationtech.jts.geom.LineString;

/**
 * Feature de linha extraída do shapefile, já em SRID 4326.
 */
public record ShapefileTrechoFeature(
	int indice,
	String codigo,
	String ruaNome,
	LineString geom
) {
}
