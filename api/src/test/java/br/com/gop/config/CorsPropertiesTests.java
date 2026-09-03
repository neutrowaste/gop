package br.com.gop.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorsPropertiesTests {

	@Test
	void origensPadraoQuandoVazio() {
		CorsProperties props = new CorsProperties();
		props.setOrigins("  ");
		assertEquals(List.of("http://localhost:4200"), props.origensPermitidas());
	}

	@Test
	void origensSeparadasPorVirgula() {
		CorsProperties props = new CorsProperties();
		props.setOrigins("https://gop.neutrowaste.cloud, http://localhost:4200");
		assertEquals(
			List.of("https://gop.neutrowaste.cloud", "http://localhost:4200"),
			props.origensPermitidas()
		);
	}
}
