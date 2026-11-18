package br.com.gop.repositories;

import org.locationtech.jts.geom.Point;

import java.time.Instant;

/** Ponto bruto do trajeto de um equipamento, na ordem em que será ligado no mapa. */
public record PosicaoTrajetoConsulta(
	Long equipamentoId,
	Point geom,
	Instant capturadoEm,
	boolean valido
) {
}
