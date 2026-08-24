package br.com.gop.services;

import br.com.gop.models.GeoJsonGeometry;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;

import static org.assertj.core.api.Assertions.assertThat;

class GeometriaWktTests {

	@Test
	void deveConverterMultiPolygonParaGeoJson() {
		MultiPolygon geom = GeometriaWkt.multiPolygonDeWkt(
			"POLYGON((-34.90 -8.05, -34.89 -8.05, -34.89 -8.04, -34.90 -8.04, -34.90 -8.05))"
		);

		GeoJsonGeometry geoJson = GeometriaWkt.paraGeoJson(geom);

		assertThat(geoJson).isNotNull();
		assertThat(geoJson.type()).isEqualTo("MultiPolygon");
		assertThat(geoJson.coordinates()).isInstanceOf(double[][][][].class);
		double[][][][] coordenadas = (double[][][][]) geoJson.coordinates();
		assertThat(coordenadas.length).isEqualTo(1);
		assertThat(coordenadas[0].length).isEqualTo(1);
		assertThat(coordenadas[0][0].length).isEqualTo(5);
		assertThat(coordenadas[0][0][0]).containsExactly(-34.90, -8.05);
	}
}
