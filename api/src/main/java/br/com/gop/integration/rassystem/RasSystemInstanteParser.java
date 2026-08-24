package br.com.gop.integration.rassystem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Datas da RasSystem chegam em formatos mistos:
 * {@code 2026-08-18 10:10:57-03} (últimas coordenadas) e
 * {@code 2026-08-18T06:25:03.000-03:00} (rastreio).
 */
public final class RasSystemInstanteParser {

	private static final List<DateTimeFormatter> COM_OFFSET = List.of(
		DateTimeFormatter.ISO_OFFSET_DATE_TIME,
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX"),
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXX"),
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"),
		DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX"),
		DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
	);

	private static final List<DateTimeFormatter> SEM_OFFSET = List.of(
		DateTimeFormatter.ISO_LOCAL_DATE_TIME,
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
	);

	private RasSystemInstanteParser() {
	}

	public static Instant parse(String bruto) {
		if (bruto == null || bruto.isBlank()) {
			return null;
		}
		String valor = bruto.strip();
		try {
			return Instant.parse(valor);
		} catch (DateTimeParseException ignored) {
			// tenta os formatos da RasSystem
		}
		for (DateTimeFormatter formatter : COM_OFFSET) {
			try {
				return OffsetDateTime.parse(valor, formatter).toInstant();
			} catch (DateTimeParseException ignored) {
				// próximo formato
			}
		}
		for (DateTimeFormatter formatter : SEM_OFFSET) {
			try {
				return LocalDateTime.parse(valor, formatter).toInstant(ZoneOffset.UTC);
			} catch (DateTimeParseException ignored) {
				// próximo formato
			}
		}
		return null;
	}

	public static Instant parseOuAgora(String bruto) {
		Instant instant = parse(bruto);
		return instant != null ? instant : Instant.now();
	}
}
