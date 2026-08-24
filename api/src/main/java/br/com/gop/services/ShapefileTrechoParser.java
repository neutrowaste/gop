package br.com.gop.services;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lê LineString/MultiLineString de um .shp (já extraído) e devolve geometria 4326.
 * Atributo padrão de via: {@code RUA}.
 */
public final class ShapefileTrechoParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(ShapefileTrechoParser.class);
	private static final int SRID = 4326;
	private static final int MAX_FEATURES = 20_000;
	private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID);

	private ShapefileTrechoParser() {
	}

	public static List<ShapefileTrechoFeature> ler(
		Path arquivoShp,
		String atributoRua,
		String atributoCodigo,
		String charsetNome
	) {
		Charset charset = charsetDe(charsetNome);
		String campoRua = atributoRua == null || atributoRua.isBlank() ? "RUA" : atributoRua.strip();
		String campoCodigo = atributoCodigo == null || atributoCodigo.isBlank() ? null : atributoCodigo.strip();
		ShapefileDataStore store = null;
		try {
			store = new ShapefileDataStore(arquivoShp.toUri().toURL());
			store.setCharset(charset);
			SimpleFeatureType schema = store.getSchema();
			String nomeRuaAttr = resolverAtributo(schema, campoRua);
			String nomeCodigoAttr = campoCodigo == null ? resolverAtributo(schema, "CODIGO", "CODE", "COD") : resolverAtributo(schema, campoCodigo);
			MathTransform paraWgs84 = transformParaWgs84(schema.getCoordinateReferenceSystem());

			List<ShapefileTrechoFeature> resultado = new ArrayList<>();
			int indice = 0;
			try (SimpleFeatureIterator it = store.getFeatureSource().getFeatures().features()) {
				while (it.hasNext()) {
					if (resultado.size() >= MAX_FEATURES) {
						throw new RegraNegocioException("Shapefile excede " + MAX_FEATURES + " trechos");
					}
					SimpleFeature feature = it.next();
					indice++;
					Geometry geometria = geometria4326(feature.getDefaultGeometry(), paraWgs84);
					List<LineString> linhas = linhasDe(geometria);
					if (linhas.isEmpty()) {
						continue;
					}
					String rua = texto(feature, nomeRuaAttr);
					String codigo = texto(feature, nomeCodigoAttr);
					for (int parte = 0; parte < linhas.size(); parte++) {
						String codigoParte = codigo;
						if (linhas.size() > 1 && codigoParte != null) {
							codigoParte = codigoParte + "-" + (parte + 1);
						}
						resultado.add(new ShapefileTrechoFeature(indice, codigoParte, rua, linhas.get(parte)));
					}
				}
			}
			LOGGER.info("Shapefile lido: arquivo={}, features={}", arquivoShp.getFileName(), resultado.size());
			return resultado;
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

	private static List<LineString> linhasDe(Geometry geometry) {
		if (geometry == null) {
			return List.of();
		}
		List<LineString> linhas = new ArrayList<>();
		if (geometry instanceof LineString lineString) {
			adicionarSeValida(linhas, lineString);
		} else if (geometry instanceof MultiLineString multi) {
			for (int i = 0; i < multi.getNumGeometries(); i++) {
				if (multi.getGeometryN(i) instanceof LineString lineString) {
					adicionarSeValida(linhas, lineString);
				}
			}
		}
		return linhas;
	}

	private static void adicionarSeValida(List<LineString> destino, LineString origem) {
		Coordinate[] origemCoords = origem.getCoordinates();
		if (origemCoords.length < 2) {
			return;
		}
		Coordinate[] xy = new Coordinate[origemCoords.length];
		for (int i = 0; i < origemCoords.length; i++) {
			xy[i] = new Coordinate(origemCoords[i].x, origemCoords[i].y);
		}
		LineString linha = FACTORY.createLineString(xy);
		linha.setSRID(SRID);
		if (!linha.isEmpty() && linha.getNumPoints() >= 2) {
			destino.add(linha);
		}
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

	private static String resolverAtributo(SimpleFeatureType schema, String... candidatos) {
		if (schema == null || candidatos == null) {
			return null;
		}
		for (String candidato : candidatos) {
			if (candidato == null || candidato.isBlank()) {
				continue;
			}
			for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
				if (descriptor.getLocalName().equalsIgnoreCase(candidato.strip())) {
					return descriptor.getLocalName();
				}
			}
		}
		return null;
	}

	private static String texto(SimpleFeature feature, String atributo) {
		if (atributo == null) {
			return null;
		}
		Object valor = feature.getAttribute(atributo);
		if (valor == null) {
			return null;
		}
		String texto = String.valueOf(valor).strip();
		return texto.isEmpty() || "null".equalsIgnoreCase(texto) ? null : texto;
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
