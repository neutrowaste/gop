package br.com.gop.services;

import br.com.gop.models.GeoJsonGeometry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

public final class GeometriaWkt {

	private static final int SRID = 4326;
	private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID);

	private GeometriaWkt() {
	}

	public static LineString lineStringDeWkt(String wkt) {
		if (wkt == null || wkt.isBlank()) {
			throw new RegraNegocioException("Geometria WKT é obrigatória");
		}
		try {
			var geometry = new WKTReader(FACTORY).read(wkt.strip());
			if (!(geometry instanceof LineString lineString) || lineString.isEmpty()) {
				throw new RegraNegocioException("Geometria deve ser um LineString WKT válido");
			}
			lineString.setSRID(SRID);
			return lineString;
		} catch (ParseException exception) {
			throw new RegraNegocioException("WKT inválido: " + exception.getMessage());
		}
	}

	public static String paraWkt(LineString lineString) {
		if (lineString == null) {
			return null;
		}
		return new WKTWriter().write(lineString);
	}

	/** Ponto GPS bruto (longitude, latitude) para ingestão de PosicaoGps. */
	public static Point pontoDe(double latitude, double longitude) {
		Point ponto = FACTORY.createPoint(new Coordinate(longitude, latitude));
		ponto.setSRID(SRID);
		return ponto;
	}

	/** Point → GeoJSON (RFC 7946): coordinates = [longitude, latitude]. */
	public static GeoJsonGeometry paraGeoJson(Point ponto) {
		if (ponto == null) {
			return null;
		}
		return new GeoJsonGeometry("Point", new double[] {ponto.getX(), ponto.getY()});
	}

	/** LineString → GeoJSON (RFC 7946): coordinates = [[longitude, latitude], ...]. */
	public static GeoJsonGeometry paraGeoJson(LineString lineString) {
		if (lineString == null) {
			return null;
		}
		Coordinate[] coordenadas = lineString.getCoordinates();
		double[][] pontos = new double[coordenadas.length][2];
		for (int indice = 0; indice < coordenadas.length; indice++) {
			pontos[indice][0] = coordenadas[indice].x;
			pontos[indice][1] = coordenadas[indice].y;
		}
		return new GeoJsonGeometry("LineString", pontos);
	}

	public static float comprimentoMetros(LineString lineString) {
		if (lineString == null) {
			return 0f;
		}
		// Aproximação geográfica: graus → metros no equador (~111_320). Suficiente para cadastro;
		// pós-fase usaremos geography/ST_Length no PostGIS.
		return (float) (lineString.getLength() * 111_320d);
	}
}
