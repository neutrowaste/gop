package br.com.gop.services;

import br.com.gop.integration.rassystem.RasFilial;
import br.com.gop.integration.rassystem.RasSystemClient;
import br.com.gop.integration.rassystem.RasSystemProperties;
import br.com.gop.models.FonteGps;
import br.com.gop.models.TelemetriaGps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RasSystemLeituraServiceTests {

	@Mock
	private RasSystemClient client;

	private final RasSystemProperties properties = new RasSystemProperties();

	private RasSystemLeituraService service;

	@BeforeEach
	void configurar() {
		when(client.properties()).thenReturn(properties);
		service = new RasSystemLeituraService(client);
	}

	@Test
	void usaFilialDaApiQuandoNaoHaConfiguracao() {
		when(client.listarFiliais()).thenReturn(List.of(new RasFilial(3606L, "Recife", "Emp")));

		assertThat(service.resolverFilial(null)).isEqualTo(3606);
		assertThat(service.filiaisParaConsulta(null)).containsExactly(3606);
	}

	@Test
	void consultaTodasAsFiliaisQuandoHouverVarias() {
		when(client.listarFiliais()).thenReturn(List.of(
			new RasFilial(3606L, "Recife", "Emp"),
			new RasFilial(3607L, "Olinda", "Emp")
		));
		TelemetriaGps ponto = new TelemetriaGps(
			FonteGps.RASSYSTEM,
			1L,
			"LUT-001",
			"LUT-001",
			-8.0,
			-34.9,
			Instant.parse("2026-08-21T15:00:00Z"),
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			List.of(),
			null,
			null,
			Map.of()
		);
		when(client.ultimasCoordenadas(3606, null, null)).thenReturn(List.of(ponto));
		when(client.ultimasCoordenadas(3607, null, null)).thenReturn(List.of());

		List<TelemetriaGps> pontos = service.ultimasCoordenadas(null, null, null);

		assertThat(pontos).containsExactly(ponto);
	}

	@Test
	void falhaQuandoNaoHaFilialNaConta() {
		when(client.listarFiliais()).thenReturn(List.of());

		assertThatThrownBy(() -> service.resolverFilial(null))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("não retornou nenhuma filial");
	}
}
