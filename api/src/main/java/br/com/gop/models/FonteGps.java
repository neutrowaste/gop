package br.com.gop.models;

/**
 * Origem da telemetria. RasSystem e GPS próprio compartilham o mesmo contrato
 * interno; a fonte só identifica o adaptador ACL que entregou o ponto.
 */
public enum FonteGps {
	MANUAL,
	RASSYSTEM,
	PROPRIO
}
