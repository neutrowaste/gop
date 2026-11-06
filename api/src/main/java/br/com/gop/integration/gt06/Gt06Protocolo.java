package br.com.gop.integration.gt06;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Codec do protocolo Concox GT06 (CRC-16/X-25, quadros 0x78 0x78).
 * O adaptador TCP usa isto e entrega {@link Gt06Posicao} ao núcleo.
 */
public final class Gt06Protocolo {

	private Gt06Protocolo() {
	}

	public static int crc(byte[] dados, int offset, int tamanho) {
		int crc = 0xFFFF;
		int fim = offset + tamanho;
		for (int i = offset; i < fim; i++) {
			crc ^= dados[i] & 0xFF;
			for (int bit = 0; bit < 8; bit++) {
				if ((crc & 0x0001) != 0) {
					crc = (crc >>> 1) ^ 0x8408;
				} else {
					crc >>>= 1;
				}
			}
		}
		return (~crc) & 0xFFFF;
	}

	public static byte[] ack(int protocolo, int serial) {
		byte[] corpo = new byte[] {
			0x05,
			(byte) protocolo,
			(byte) (serial >>> 8),
			(byte) serial
		};
		int crc = crc(corpo, 0, corpo.length);
		return new byte[] {
			0x78,
			0x78,
			0x05,
			(byte) protocolo,
			(byte) (serial >>> 8),
			(byte) serial,
			(byte) (crc >>> 8),
			(byte) crc,
			0x0D,
			0x0A
		};
	}

	/**
	 * O rastreador espera confirmação de quase todo quadro que envia.
	 * 0x15 é resposta a comando nosso; 0x80/0x81 nascem no servidor.
	 */
	public static boolean exigeAck(int protocolo) {
		return protocolo != 0x15 && protocolo != 0x80 && protocolo != 0x81;
	}

	public static Optional<String> imei(Gt06Quadro quadro) {
		if (quadro.protocolo() != 0x01 || quadro.info().length < 8) {
			return Optional.empty();
		}
		StringBuilder digitos = new StringBuilder(16);
		for (int i = 0; i < 8; i++) {
			int byteImei = quadro.info()[i] & 0xFF;
			digitos.append(byteImei >>> 4);
			digitos.append(byteImei & 0x0F);
		}
		String imei = digitos.toString();
		if (imei.length() == 16 && imei.startsWith("0")) {
			imei = imei.substring(1);
		}
		return Optional.of(imei);
	}

	public static Optional<Gt06Posicao> posicao(Gt06Quadro quadro) {
		if (!temGps(quadro.protocolo()) || quadro.info().length < 18) {
			return Optional.empty();
		}
		return lerGps(quadro.info());
	}

	public static String hex(byte[] dados, int limite) {
		int n = Math.min(dados.length, limite);
		StringBuilder texto = new StringBuilder(n * 3);
		for (int i = 0; i < n; i++) {
			if (i > 0) {
				texto.append(' ');
			}
			texto.append(Character.forDigit((dados[i] >>> 4) & 0x0F, 16));
			texto.append(Character.forDigit(dados[i] & 0x0F, 16));
		}
		if (dados.length > n) {
			texto.append(" …");
		}
		return texto.toString();
	}

	private static boolean temGps(int protocolo) {
		return switch (protocolo) {
			case 0x12, 0x16, 0x22, 0x26, 0x27, 0xA0 -> true;
			default -> false;
		};
	}

	private static Optional<Gt06Posicao> lerGps(byte[] info) {
		int ano = 2000 + (info[0] & 0xFF);
		int mes = info[1] & 0xFF;
		int dia = info[2] & 0xFF;
		int hora = info[3] & 0xFF;
		int minuto = info[4] & 0xFF;
		int segundo = info[5] & 0xFF;
		int satelites = info[6] & 0x0F;
		double latitude = inteiro(info, 7) / 60.0 / 30_000.0;
		double longitude = inteiro(info, 11) / 60.0 / 30_000.0;
		int velocidade = info[15] & 0xFF;
		int flags = ((info[16] & 0xFF) << 8) | (info[17] & 0xFF);
		if ((flags & 0x0400) == 0) {
			latitude = -latitude;
		}
		if ((flags & 0x0800) != 0) {
			longitude = -longitude;
		}
		if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0) {
			return Optional.empty();
		}
		// Bit 12 indica "GPS já posicionou" no GT06 clássico. Neste rastreador ele
		// vem desligado mesmo com coordenada real e estável, então a posição
		// entra no mapa. Só descartamos 0,0, que é ausência de leitura.
		boolean origemZero = latitude == 0.0 && longitude == 0.0;
		LocalDateTime dataHora;
		try {
			dataHora = LocalDateTime.of(ano, mes, dia, hora, minuto, segundo);
		} catch (DateTimeException excecao) {
			return Optional.empty();
		}
		return Optional.of(new Gt06Posicao(
			latitude,
			longitude,
			dataHora.toInstant(ZoneOffset.UTC),
			velocidade,
			flags & 0x03FF,
			satelites,
			!origemZero
		));
	}

	private static long inteiro(byte[] dados, int offset) {
		return ((dados[offset] & 0xFFL) << 24)
			| ((dados[offset + 1] & 0xFFL) << 16)
			| ((dados[offset + 2] & 0xFFL) << 8)
			| (dados[offset + 3] & 0xFFL);
	}
}
