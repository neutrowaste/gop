package br.com.gop.services;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê Polygon/MultiPolygon de um .shp (já extraído) e devolve um MultiPolygon 4326.
 */
public final class ShapefileSetorParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(ShapefileSetorParser.class);
	private static final int SRID = 4326;
	private static final int MAX_FEATURES = 2_000;
	private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID);

	private ShapefileSetorParser() {
	}

	public static MultiPolygon ler(Path arquivoShp, String charsetNome) {
		Charset charset = charsetDe(charsetNome);
		ShapefileDataStore store = null;
		try {
			store = new ShapefileDataStore(arquivoShp.toUri().toURL());
			store.setCharset(charset);
			SimpleFeatureType schema = store.getSchema();
			MathTransform paraWgs84 = transformParaWgs84(schema.getCoordinateReferenceSystem());

			List<Polygon> poligonos = new ArrayList<>();
			int indice = 0;
			try (var it = store.getFeatureSource().getFeatures().features()) {
				while (it.hasNext()) {
					if (indice >= MAX_FEATURES) {
						throw new RegraNegocioException("Shapefile excede " + MAX_FEATURES + " polígonos");
					}
					SimpleFeature feature = it.next();
					indice++;
					Geometry geometria = geometria4326(feature.getDefaultGeometry(), paraWgs84);
					poligonos.addAll(poligonosDe(geometria));
				}
			}
			if (poligonos.isEmpty()) {
				throw new RegraNegocioException(
					"O shapefile não contém polígonos. Envie um arquivo de área (Polygon/MultiPolygon)."
				);
			}
			MultiPolygon demarcacao = unir(poligonos);
			LOGGER.info(
				"Shapefile de setor lido: arquivo={}, features={}, partes={}",
				arquivoShp.getFileName(),
				indice,
				demarcacao.getNumGeometries()
			);
			return demarcacao;
		} catch (RegraNegocioException exception) {
			throw exception;
		} catch (IOException | FactoryException | TransformException exception) {
			throw new RegraNegocioException("Falha ao ler shapefile: " + exception.getMessage());
		} finally {
			if (store != null) {
				store.dispose();
			}
		}
	}

	private static Geometry geometria4326(Object bruto, MathTransform paraWgs84) throws TransformException {
		if (!(bruto instanceof Geometry geometry) || geometry.isEmpty()) {
			return null;
		}
		Geometry copia = geometry.copy();
		if (paraWgs84 != null) {
			copia = JTS.transform(copia, paraWgs84);
		}
		return copia;
	}

	private static List<Polygon> poligonosDe(Geometry geometry) {
		if (geometry == null || geometry.isEmpty()) {
			return List.of();
		}
		if (geometry instanceof LineString || geometry instanceof MultiLineString) {
			throw new RegraNegocioException(
				"O shapefile é de linha. A demarcação do setor exige um polígono de área."
			);
		}
		List<Polygon> poligonos = new ArrayList<>();
		if (geometry instanceof Polygon polygon) {
			adicionarSeValido(poligonos, polygon);
		} else if (geometry instanceof MultiPolygon multi) {
			for (int i = 0; i < multi.getNumGeometries(); i++) {
				if (multi.getGeometryN(i) instanceof Polygon polygon) {
					adicionarSeValido(poligonos, polygon);
				}
			}
		} else {
			for (int i = 0; i < geometry.getNumGeometries(); i++) {
				Geometry parte = geometry.getGeometryN(i);
				if (parte instanceof Polygon || parte instanceof MultiPolygon) {
					poligonos.addAll(poligonosDe(parte));
				}
			}
		}
		return poligonos;
	}

	private static void adicionarSeValido(List<Polygon> destino, Polygon origem) {
		Polygon xy = poligono2d(origem);
		if (!xy.isEmpty() && xy.getNumPoints() >= 4) {
			destino.add(xy);
		}
	}

	private static Polygon poligono2d(Polygon origem) {
		LinearRing shell = anel2d(origem.getExteriorRing());
		LinearRing[] buracos = new LinearRing[origem.getNumInteriorRing()];
		for (int i = 0; i < origem.getNumInteriorRing(); i++) {
			buracos[i] = anel2d(origem.getInteriorRingN(i));
		}
		Polygon poligono = FACTORY.createPolygon(shell, buracos);
		poligono.setSRID(SRID);
		return poligono;
	}

	private static LinearRing anel2d(LineString anel) {
		Coordinate[] origem = anel.getCoordinates();
		Coordinate[] xy = new Coordinate[origem.length];
		for (int i = 0; i < origem.length; i++) {
			xy[i] = new Coordinate(origem[i].x, origem[i].y);
		}
		return FACTORY.createLinearRing(xy);
	}

	private static MultiPolygon unir(List<Polygon> poligonos) {
		Geometry unionada = UnaryUnionOp.union(poligonos);
		if (unionada instanceof Polygon polygon) {
			MultiPolygon multi = FACTORY.createMultiPolygon(new Polygon[] {polygon});
			multi.setSRID(SRID);
			return multi;
		}
		if (unionada instanceof MultiPolygon multi) {
			multi.setSRID(SRID);
			return multi;
		}
		List<Polygon> partes = poligonosDe(unionada);
		if (partes.isEmpty()) {
			throw new RegraNegocioException("Não foi possível unir os polígonos do shapefile");
		}
		MultiPolygon multi = FACTORY.createMultiPolygon(partes.toArray(Polygon[]::new));
		multi.setSRID(SRID);
		return multi;
	}

	private static MathTransform transformParaWgs84(CoordinateReferenceSystem origem) throws FactoryException {
		if (origem == null) {
			return null;
		}
		CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
		if (CRS.equalsIgnoreMetadata(origem, wgs84)) {
			return null;
		}
		return CRS.findMathTransform(origem, wgs84, true);
	}

	private static Charset charsetDe(String nome) {
		if (nome == null || nome.isBlank()) {
			return Charset.forName("ISO-8859-1");
		}
		try {
			return Charset.forName(nome.strip());
		} catch (Exception ignored) {
			return StandardCharsets.UTF_8;
		}
	}
}
