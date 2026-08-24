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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShapefileTrechoParserTests {

	private static final GeometryFactory FACTORY = new GeometryFactory();

	@TempDir
	Path temp;

	@Test
	void leAtributosRuaECodigo() throws Exception {
		Path shp = escreverShapefile(temp.resolve("vias.shp"), "RUA CONSOLACAO", "TR-10");
		List<ShapefileTrechoFeature> features = ShapefileTrechoParser.ler(shp, "RUA", "CODIGO", "UTF-8");
		assertThat(features).hasSize(1);
		assertThat(features.get(0).ruaNome()).isEqualTo("RUA CONSOLACAO");
		assertThat(features.get(0).codigo()).isEqualTo("TR-10");
		assertThat(features.get(0).geom().getSRID()).isEqualTo(4326);
		assertThat(features.get(0).geom().getNumPoints()).isGreaterThanOrEqualTo(2);
	}

	@Test
	void zipSemShpERejeitado() throws Exception {
		ByteArrayOutputStream zip = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(zip)) {
			zos.putNextEntry(new ZipEntry("leia-me.txt"));
			zos.write("nao e shapefile".getBytes());
			zos.closeEntry();
		}
		assertThatThrownBy(() -> ShapefileZip.extrair(new ByteArrayInputStream(zip.toByteArray()), temp.resolve("out")))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining(".shp");
	}

	@Test
	void zipComShpEExtraido() throws Exception {
		Path shp = escreverShapefile(temp.resolve("origem").resolve("vias.shp"), "AV BOA VIAGEM", "AV-1");
		ByteArrayOutputStream zip = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(zip)) {
			for (String ext : List.of(".shp", ".shx", ".dbf", ".prj")) {
				Path arquivo = Path.of(shp.toString().replace(".shp", ext));
				if (!Files.exists(arquivo)) {
					continue;
				}
				zos.putNextEntry(new ZipEntry("vias" + ext));
				Files.copy(arquivo, zos);
				zos.closeEntry();
			}
		}
		Path extraido = ShapefileZip.extrair(new ByteArrayInputStream(zip.toByteArray()), temp.resolve("unzip"));
		assertThat(extraido.getFileName().toString()).endsWith(".shp");
		assertThat(ShapefileTrechoParser.ler(extraido, "RUA", "CODIGO", "UTF-8")).hasSize(1);
	}

	private static Path escreverShapefile(Path shp, String rua, String codigo) throws Exception {
		Files.createDirectories(shp.getParent());
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("vias");
		builder.setCRS(DefaultGeographicCRS.WGS84);
		builder.add("the_geom", LineString.class);
		builder.length(80).add("RUA", String.class);
		builder.length(40).add("CODIGO", String.class);
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
			feature.setAttribute("RUA", rua);
			feature.setAttribute("CODIGO", codigo);
			writer.write();
		} finally {
			store.dispose();
		}
		return shp;
	}
}
