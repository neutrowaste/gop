package br.com.gop.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Retrato agregado do mapa operacional (RN-001) para um dia: ativos (última
 * posição), trechos (plano + status) e desvios (trajeto real fora do eixo)
 * de todas as Escalas PUBLICADA da empresa.
 */
public record MapaOperacionalResponse(
	LocalDate data,
	Instant geradoEm,
	List<MapaAtivoResponse> ativos,
	List<MapaTrechoResponse> trechos,
	List<MapaDesvioResponse> desvios
) {
}
