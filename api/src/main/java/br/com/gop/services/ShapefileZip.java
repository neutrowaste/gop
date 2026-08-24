package br.com.gop.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extrai um ZIP de shapefile (.shp/.shx/.dbf/.prj/.cpg) com proteção contra zip-slip.
 */
final class ShapefileZip {

	private static final Set<String> EXTENSOES = Set.of(".shp", ".shx", ".dbf", ".prj", ".cpg", ".sbn", ".sbx");
	private static final int MAX_ARQUIVOS = 40;
	private static final long MAX_BYTES = 200L * 1024 * 1024;

	private ShapefileZip() {
	}

	static Path extrair(InputStream zip, Path destino) throws IOException {
		Files.createDirectories(destino);
		Path raiz = destino.toAbsolutePath().normalize();
		int arquivos = 0;
		long total = 0;
		try (ZipInputStream zis = new ZipInputStream(zip)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				String nome = Path.of(entry.getName()).getFileName().toString();
				String ext = extensao(nome);
				if (!EXTENSOES.contains(ext)) {
					continue;
				}
				arquivos++;
				if (arquivos > MAX_ARQUIVOS) {
					throw new RegraNegocioException("ZIP do shapefile contém arquivos demais");
				}
				Path alvo = raiz.resolve(nome).normalize();
				if (!alvo.startsWith(raiz)) {
					throw new RegraNegocioException("ZIP inválido");
				}
				try (var out = Files.newOutputStream(alvo)) {
					byte[] buffer = new byte[8192];
					int lido;
					while ((lido = zis.read(buffer)) >= 0) {
						total += lido;
						if (total > MAX_BYTES) {
							throw new RegraNegocioException("Shapefile descompactado excede o limite de 200 MB");
						}
						out.write(buffer, 0, lido);
					}
				}
			}
		}
		Path shp = null;
		try (var stream = Files.list(raiz)) {
			shp = stream
				.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".shp"))
				.findFirst()
				.orElse(null);
		}
		if (shp == null) {
			throw new RegraNegocioException(
				"O ZIP precisa conter um arquivo .shp (e de preferência .shx, .dbf e .prj)"
			);
		}
		return shp;
	}

	private static String extensao(String nome) {
		int ponto = nome.lastIndexOf('.');
		if (ponto < 0) {
			return "";
		}
		return nome.substring(ponto).toLowerCase(Locale.ROOT);
	}
}
