package br.com.gop.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Contrato interno de um ponto GPS, independente da fonte (RasSystem, GPS
 * próprio ou ingestão manual). O adaptador ACL preenche todos os campos
 * que o dispositivo enviar; o motor de regras consome lat/lng/tempo/velocidade.
 */
public record TelemetriaGps(
	FonteGps fonte,
	Long idExterno,
	String codigoLogico,
	String rotuloVeiculo,
	double latitude,
	double longitude,
	Instant capturadoEm,
	Float velocidadeKmh,
	Float direcaoGraus,
	Float altitudeMetros,
	Long odometro,
	Integer satelites,
	Long rd,
	String situacao,
	Integer tipoServico,
	Integer modelo,
	String setor,
	List<Integer> rotulos,
	String tipoEvento,
	String valorEvento,
	Map<String, Object> propriedades
) {
}
