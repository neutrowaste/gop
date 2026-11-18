package br.com.gop.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Liga posições GPS em linhas só onde o veículo saiu do trecho ({@code valido = false}).
 * O ponto válido imediatamente antes da saída e o primeiro válido na volta entram na
 * linha, para o desenho sair da rua prevista e retornar a ela. Um buraco maior que o
 * intervalo máximo quebra a linha, sem inventar uma reta no período sem sinal.
 */
public final class TrajetoDesvio {

	/** Pontos mais próximos que isso, com a mesma validade, não vão para o mapa. */
	static final double DISTANCIA_MINIMA_METROS = 12d;

	private TrajetoDesvio() {
	}

	public record Ponto(
		long equipamentoId,
		double longitude,
		double latitude,
		Instant capturadoEm,
		boolean valido
	) {
	}

	public record Linha(long equipamentoId, Instant saidaEm, double[][] coordenadas) {
	}

	public static List<Linha> linhasForaDoTrecho(List<Ponto> pontos, Duration intervaloMaximo) {
		Map<Long, List<Ponto>> porEquipamento = new LinkedHashMap<>();
		for (Ponto ponto : pontos) {
			porEquipamento.computeIfAbsent(ponto.equipamentoId(), id -> new ArrayList<>()).add(ponto);
		}
		List<Linha> linhas = new ArrayList<>();
		for (Map.Entry<Long, List<Ponto>> entrada : porEquipamento.entrySet()) {
			List<Ponto> ordenados = entrada.getValue().stream()
				.sorted(Comparator.comparing(Ponto::capturadoEm))
				.toList();
			linhas.addAll(linhasDe(entrada.getKey(), ordenados, intervaloMaximo));
		}
		return linhas;
	}

	private static List<Linha> linhasDe(long equipamentoId, List<Ponto> pontos, Duration intervaloMaximo) {
		List<Linha> linhas = new ArrayList<>();
		List<Ponto> corrida = new ArrayList<>();
		Ponto anteriorValido = null;
		Ponto anterior = null;
		for (Ponto ponto : pontos) {
			boolean buraco = anterior != null
				&& Duration.between(anterior.capturadoEm(), ponto.capturadoEm()).compareTo(intervaloMaximo) > 0;
			if (buraco) {
				emitir(linhas, equipamentoId, corrida);
				corrida = new ArrayList<>();
				anteriorValido = null;
			}
			if (ponto.valido()) {
				if (!corrida.isEmpty()) {
					corrida.add(ponto);
					emitir(linhas, equipamentoId, corrida);
					corrida = new ArrayList<>();
				}
				anteriorValido = ponto;
			} else {
				if (corrida.isEmpty() && anteriorValido != null) {
					corrida.add(anteriorValido);
				}
				corrida.add(ponto);
			}
			anterior = ponto;
		}
		emitir(linhas, equipamentoId, corrida);
		return linhas;
	}

	private static void emitir(List<Linha> destino, long equipamentoId, List<Ponto> corrida) {
		if (corrida.stream().noneMatch(ponto -> !ponto.valido())) {
			return;
		}
		List<Ponto> simplificada = simplificar(corrida);
		if (simplificada.size() < 2) {
			return;
		}
		Instant saidaEm = simplificada.stream()
			.filter(ponto -> !ponto.valido())
			.map(Ponto::capturadoEm)
			.findFirst()
			.orElse(simplificada.get(0).capturadoEm());
		double[][] coordenadas = new double[simplificada.size()][2];
		for (int indice = 0; indice < simplificada.size(); indice++) {
			Ponto ponto = simplificada.get(indice);
			coordenadas[indice][0] = ponto.longitude();
			coordenadas[indice][1] = ponto.latitude();
		}
		destino.add(new Linha(equipamentoId, saidaEm, coordenadas));
	}

	static List<Ponto> simplificar(List<Ponto> pontos) {
		if (pontos.size() <= 2) {
			return pontos;
		}
		List<Ponto> saida = new ArrayList<>();
		saida.add(pontos.get(0));
		for (int indice = 1; indice < pontos.size() - 1; indice++) {
			Ponto candidato = pontos.get(indice);
			Ponto ultimo = saida.get(saida.size() - 1);
			boolean mesmaValidade = candidato.valido() == ultimo.valido();
			if (!mesmaValidade || distanciaMetros(ultimo, candidato) >= DISTANCIA_MINIMA_METROS) {
				saida.add(candidato);
			}
		}
		Ponto fim = pontos.get(pontos.size() - 1);
		Ponto ultimo = saida.get(saida.size() - 1);
		if (distanciaMetros(ultimo, fim) < 1d && ultimo.valido() == fim.valido()) {
			saida.set(saida.size() - 1, fim);
		} else {
			saida.add(fim);
		}
		return saida;
	}

	private static double distanciaMetros(Ponto a, Ponto b) {
		double radiano = Math.PI / 180d;
		double latitudeA = a.latitude() * radiano;
		double latitudeB = b.latitude() * radiano;
		double deltaLatitude = latitudeB - latitudeA;
		double deltaLongitude = (b.longitude() - a.longitude()) * radiano;
		double h = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
			+ Math.cos(latitudeA) * Math.cos(latitudeB) * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
		return 2 * 6_371_000d * Math.asin(Math.min(1d, Math.sqrt(h)));
	}
}
