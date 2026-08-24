package br.com.gop.services;

import br.com.gop.models.GeoJsonGeometry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
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

	public static String paraWkt(Geometry geometry) {
		if (geometry == null) {
			return null;
		}
		return new WKTWriter().write(geometry);
	}

	public static MultiPolygon multiPolygonDeWkt(String wkt) {
		if (wkt == null || wkt.isBlank()) {
			throw new RegraNegocioException("Geometria WKT é obrigatória");
		}
		try {
			Geometry geometry = new WKTReader(FACTORY).read(wkt.strip());
			if (geometry instanceof MultiPolygon multi && !multi.isEmpty()) {
				multi.setSRID(SRID);
				return multi;
			}
			if (geometry instanceof Polygon polygon && !polygon.isEmpty() && polygon.getNumPoints() >= 4) {
				MultiPolygon multi = FACTORY.createMultiPolygon(new Polygon[] {polygon});
				multi.setSRID(SRID);
				return multi;
			}
			throw new RegraNegocioException("Geometria deve ser um Polygon ou MultiPolygon WKT válido");
		} catch (ParseException exception) {
			throw new RegraNegocioException("WKT inválido: " + exception.getMessage());
		}
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

	public static GeoJsonGeometry paraGeoJson(Geometry geometry) {
		if (geometry == null || geometry.isEmpty()) {
			return null;
		}
		if (geometry instanceof Point ponto) {
			return paraGeoJson(ponto);
		}
		if (geometry instanceof LineString linha && !(geometry instanceof LinearRing)) {
			return paraGeoJson(linha);
		}
		if (geometry instanceof Polygon poligono) {
			return new GeoJsonGeometry("Polygon", coordenadasPolygon(poligono));
		}
		if (geometry instanceof MultiPolygon multi) {
			return new GeoJsonGeometry("MultiPolygon", coordenadasMultiPolygon(multi));
		}
		return null;
	}

	private static double[][] anel(LineString ring) {
		Coordinate[] coordenadas = ring.getCoordinates();
		double[][] pontos = new double[coordenadas.length][2];
		for (int indice = 0; indice < coordenadas.length; indice++) {
			pontos[indice][0] = coordenadas[indice].x;
			pontos[indice][1] = coordenadas[indice].y;
		}
		return pontos;
	}

	private static double[][][] coordenadasPolygon(Polygon polygon) {
		double[][][] aneis = new double[1 + polygon.getNumInteriorRing()][][];
		aneis[0] = anel(polygon.getExteriorRing());
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			aneis[i + 1] = anel(polygon.getInteriorRingN(i));
		}
		return aneis;
	}

	private static double[][][][] coordenadasMultiPolygon(MultiPolygon multi) {
		double[][][][] poligonos = new double[multi.getNumGeometries()][][][];
		for (int i = 0; i < multi.getNumGeometries(); i++) {
			poligonos[i] = coordenadasPolygon((Polygon) multi.getGeometryN(i));
		}
		return poligonos;
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
