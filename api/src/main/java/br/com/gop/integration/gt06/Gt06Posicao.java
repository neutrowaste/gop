package br.com.gop.integration.gt06;

import java.time.Instant;

/**
 * Ponto extraído de um quadro de localização GT06 (0x12, 0x16, 0x22, 0xA0).
 */
public record Gt06Posicao(
	double latitude,
	double longitude,
	Instant capturadoEm,
	float velocidadeKmh,
	float direcaoGraus,
	int satelites,
	boolean valido
) {
}
