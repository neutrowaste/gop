package br.com.gop.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Retrato agregado do mapa operacional (RN-001) para um dia: ativos (última
 * posição) e trechos (plano + status) de todas as Escalas PUBLICADA da empresa.
 */
public record MapaOperacionalResponse(
	LocalDate data,
	Instant geradoEm,
	List<MapaAtivoResponse> ativos,
	List<MapaTrechoResponse> trechos
) {
}
