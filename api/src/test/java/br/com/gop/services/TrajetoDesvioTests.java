package br.com.gop.services;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrajetoDesvioTests {

	private static final Instant T0 = Instant.parse("2026-11-17T10:00:00Z");
	private static final Duration INTERVALO = Duration.ofMinutes(30);

	@Test
	void naoDesenhaLinhaQuandoOGpsPermaneceNoTrecho() {
		List<TrajetoDesvio.Linha> linhas = TrajetoDesvio.linhasForaDoTrecho(List.of(
			ponto(1, -46.630, -23.550, 0, true),
			ponto(1, -46.630, -23.551, 1, true),
			ponto(1, -46.630, -23.552, 2, true)
		), INTERVALO);

		assertThat(linhas).isEmpty();
	}

	@Test
	void ligaASaidaAoRetornoQuandoOGpsDeixaOTrecho() {
		List<TrajetoDesvio.Linha> linhas = TrajetoDesvio.linhasForaDoTrecho(List.of(
			ponto(1, -46.630, -23.550, 0, true),
			ponto(1, -46.631, -23.551, 1, false),
			ponto(1, -46.632, -23.552, 2, false),
			ponto(1, -46.630, -23.553, 3, true)
		), INTERVALO);

		assertThat(linhas).hasSize(1);
		assertThat(linhas.get(0).equipamentoId()).isEqualTo(1L);
		assertThat(linhas.get(0).coordenadas()).hasDimensions(4, 2);
		assertThat(linhas.get(0).coordenadas()[0]).containsExactly(-46.630, -23.550);
		assertThat(linhas.get(0).coordenadas()[3]).containsExactly(-46.630, -23.553);
	}

	@Test
	void quebraALinhaQuandoOSinalFicaMudoAlemDaTolerancia() {
		List<TrajetoDesvio.Linha> linhas = TrajetoDesvio.linhasForaDoTrecho(List.of(
			ponto(1, -46.630, -23.550, 0, true),
			ponto(1, -46.631, -23.551, 1, false),
			ponto(1, -46.640, -23.560, 40, false),
			ponto(1, -46.641, -23.561, 41, false)
		), INTERVALO);

		assertThat(linhas).hasSize(2);
		assertThat(linhas.get(0).coordenadas()).hasDimensions(2, 2);
		assertThat(linhas.get(0).coordenadas()[1]).containsExactly(-46.631, -23.551);
		assertThat(linhas.get(1).coordenadas()[0]).containsExactly(-46.640, -23.560);
	}

	@Test
	void descartaPontosForaDoTrechoMuitoProximosDaMesmaValidade() {
		List<TrajetoDesvio.Ponto> corrida = List.of(
			ponto(1, -46.63000, -23.55000, 0, true),
			ponto(1, -46.63100, -23.55100, 1, false),
			ponto(1, -46.63102, -23.55102, 2, false),
			ponto(1, -46.63300, -23.55300, 3, false)
		);

		List<TrajetoDesvio.Ponto> simplificada = TrajetoDesvio.simplificar(corrida);

		assertThat(simplificada).extracting(TrajetoDesvio.Ponto::latitude)
			.containsExactly(-23.55000, -23.55100, -23.55300);
	}

	private static TrajetoDesvio.Ponto ponto(
		long equipamentoId,
		double longitude,
		double latitude,
		long minutos,
		boolean valido
	) {
		return new TrajetoDesvio.Ponto(equipamentoId, longitude, latitude, T0.plus(Duration.ofMinutes(minutos)), valido);
	}
}
