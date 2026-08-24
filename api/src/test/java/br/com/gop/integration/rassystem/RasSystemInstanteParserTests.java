package br.com.gop.integration.rassystem;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RasSystemInstanteParserTests {

	@Test
	void parseUltimasCoordenadasComOffsetCurto() {
		Instant instant = RasSystemInstanteParser.parse("2026-08-18 10:10:57-03");
		assertThat(instant).isEqualTo(OffsetDateTime.parse("2026-08-18T10:10:57-03:00").toInstant());
	}

	@Test
	void parseRastreioIsoComMillis() {
		Instant instant = RasSystemInstanteParser.parse("2026-08-18T06:25:03.000-03:00");
		assertThat(instant).isEqualTo(OffsetDateTime.parse("2026-08-18T06:25:03-03:00").toInstant());
	}

	@Test
	void parseInstantIso() {
		Instant instant = RasSystemInstanteParser.parse("2026-08-18T13:10:57Z");
		assertThat(instant).isEqualTo(Instant.parse("2026-08-18T13:10:57Z"));
	}

	@Test
	void parseVazioRetornaNulo() {
		assertThat(RasSystemInstanteParser.parse("")).isNull();
		assertThat(RasSystemInstanteParser.parse(null)).isNull();
	}
}
