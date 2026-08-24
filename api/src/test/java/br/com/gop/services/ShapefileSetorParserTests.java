package br.com.gop.services;

import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShapefileSetorParserTests {

	private static final GeometryFactory FACTORY = new GeometryFactory();

	@TempDir
	Path temp;

	@Test
	void lePoligonoComoMultiPolygon4326() throws Exception {
		Path shp = escreverPoligono(temp.resolve("area.shp"));
		MultiPolygon geom = ShapefileSetorParser.ler(shp, "UTF-8");
		assertThat(geom.getSRID()).isEqualTo(4326);
		assertThat(geom.getNumGeometries()).isGreaterThanOrEqualTo(1);
		assertThat(geom.getArea()).isGreaterThan(0);
	}

	@Test
	void rejeitaShapefileDeLinha() throws Exception {
		Path shp = escreverLinha(temp.resolve("vias.shp"));
		assertThatThrownBy(() -> ShapefileSetorParser.ler(shp, "UTF-8"))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("polígono");
	}

	private static Path escreverPoligono(Path shp) throws Exception {
		Files.createDirectories(shp.getParent());
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("area");
		builder.setCRS(DefaultGeographicCRS.WGS84);
		builder.add("the_geom", Polygon.class);
		SimpleFeatureType schema = builder.buildFeatureType();

		ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
		ShapefileDataStore store = (ShapefileDataStore) factory.createNewDataStore(
			Map.of("url", shp.toUri().toURL())
		);
		store.createSchema(schema);
		Polygon poligono = FACTORY.createPolygon(new Coordinate[] {
			new Coordinate(-34.90, -8.05),
			new Coordinate(-34.89, -8.05),
			new Coordinate(-34.89, -8.04),
			new Coordinate(-34.90, -8.04),
			new Coordinate(-34.90, -8.05)
		});
		try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
				store.getFeatureWriterAppend(store.getTypeNames()[0], Transaction.AUTO_COMMIT)) {
			SimpleFeature feature = writer.next();
			feature.setAttribute("the_geom", poligono);
			writer.write();
		} finally {
			store.dispose();
		}
		return shp;
	}

	private static Path escreverLinha(Path shp) throws Exception {
		Files.createDirectories(shp.getParent());
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("vias");
		builder.setCRS(DefaultGeographicCRS.WGS84);
		builder.add("the_geom", LineString.class);
		SimpleFeatureType schema = builder.buildFeatureType();

		ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
		ShapefileDataStore store = (ShapefileDataStore) factory.createNewDataStore(
			Map.of("url", shp.toUri().toURL())
		);
		store.createSchema(schema);
		LineString linha = FACTORY.createLineString(new Coordinate[] {
			new Coordinate(-34.881, -8.047),
			new Coordinate(-34.882, -8.048)
		});
		try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
				store.getFeatureWriterAppend(store.getTypeNames()[0], Transaction.AUTO_COMMIT)) {
			SimpleFeature feature = writer.next();
			feature.setAttribute("the_geom", linha);
			writer.write();
		} finally {
			store.dispose();
		}
		return shp;
	}
}
