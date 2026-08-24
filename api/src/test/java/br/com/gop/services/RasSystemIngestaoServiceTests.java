package br.com.gop.services;

import br.com.gop.integration.rassystem.RasSystemClient;
import br.com.gop.models.Equipamento;
import br.com.gop.models.EquipamentoRequest;
import br.com.gop.models.EquipamentoResponse;
import br.com.gop.models.FonteGps;
import br.com.gop.models.TelemetriaGps;
import br.com.gop.repositories.PosicaoGpsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RasSystemIngestaoServiceTests {

	@Mock
	private RasSystemClient client;

	@Mock
	private RasSystemLeituraService leituraService;

	@Mock
	private EquipamentoService equipamentoService;

	@Mock
	private GpsIngestaoService gpsIngestaoService;

	@Mock
	private PosicaoGpsRepository posicaoGpsRepository;

	private RasSystemIngestaoService service;

	@BeforeEach
	void configurar() {
		service = new RasSystemIngestaoService(
			client,
			leituraService,
			equipamentoService,
			gpsIngestaoService,
			posicaoGpsRepository
		);
	}

	@Test
	void importarCadastroCriaLutocarESincroniza() {
		Equipamento equipamento = new Equipamento();
		equipamento.setId(10L);
		equipamento.setCodigo("LUT-029");
		equipamento.setAtivo(true);
		TelemetriaGps ponto = telemetria(37280L, "LUT-029", -8.06, -34.93);

		when(leituraService.resolverRotulo(null)).thenReturn(8018);
		when(leituraService.ultimasCoordenadas(any(), any(), any())).thenReturn(List.of(ponto));
		when(client.listarVeiculos(8018L)).thenReturn(List.of());
		when(equipamentoService.listarEntidades(1L))
			.thenReturn(List.of())
			.thenReturn(List.of(equipamento));
		when(equipamentoService.criar(eq(1L), any(EquipamentoRequest.class)))
			.thenReturn(new EquipamentoResponse(10L, "LUT-029", "LUT-029", "37280", FonteGps.RASSYSTEM, true));
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(1L, 10L))
			.thenReturn(Optional.empty());

		var resultado = service.importarCadastro(1L, 5, null, null);

		assertThat(resultado.criados()).isEqualTo(1);
		assertThat(resultado.jaExistentes()).isZero();
		assertThat(resultado.sync().ingeridos()).isEqualTo(1);
		verify(equipamentoService).criar(eq(1L), any(EquipamentoRequest.class));
		verify(gpsIngestaoService).ingerir(eq(1L), eq(equipamento), eq(ponto));
	}

	@Test
	void importarCadastroNaoRecriaLutocarJaVinculado() {
		Equipamento equipamento = new Equipamento();
		equipamento.setId(10L);
		equipamento.setCodigo("LUT-029");
		equipamento.setCodigoExterno("37280");
		equipamento.setAtivo(true);
		TelemetriaGps ponto = telemetria(37280L, "LUT-029", -8.06, -34.93);

		when(leituraService.resolverRotulo(null)).thenReturn(8018);
		when(leituraService.ultimasCoordenadas(any(), any(), any())).thenReturn(List.of(ponto));
		when(client.listarVeiculos(8018L)).thenReturn(List.of());
		when(equipamentoService.listarEntidades(1L)).thenReturn(List.of(equipamento));
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(1L, 10L))
			.thenReturn(Optional.empty());

		var resultado = service.importarCadastro(1L, 5, null, null);

		assertThat(resultado.criados()).isZero();
		assertThat(resultado.sync().ingeridos()).isEqualTo(1);
		verify(equipamentoService, never()).criar(any(), any());
	}

	@Test
	void syncIngereLutocarJaCadastrado() {
		Equipamento equipamento = new Equipamento();
		equipamento.setId(10L);
		equipamento.setCodigo("LUT-029");
		equipamento.setAtivo(true);
		TelemetriaGps ponto = telemetria(37280L, "LUT-029", -8.06, -34.93);

		when(leituraService.ultimasCoordenadas(3606, 8018, null)).thenReturn(List.of(ponto));
		when(equipamentoService.listarEntidades(1L)).thenReturn(List.of(equipamento));
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(1L, 10L))
			.thenReturn(Optional.empty());

		var resultado = service.sincronizar(1L, 3606, 8018);

		assertThat(resultado.ingeridos()).isEqualTo(1);
		assertThat(resultado.naoVinculados()).isZero();
		verify(gpsIngestaoService).ingerir(eq(1L), eq(equipamento), eq(ponto));
		verify(equipamentoService).vincularExterno(equipamento, "37280", FonteGps.RASSYSTEM);
	}

	@Test
	void syncNaoCriaEquipamentoQuandoLutNaoCadastrado() {
		TelemetriaGps ponto = telemetria(37280L, "LUT-029", -8.06, -34.93);
		when(leituraService.ultimasCoordenadas(null, null, null)).thenReturn(List.of(ponto));
		when(equipamentoService.listarEntidades(1L)).thenReturn(List.of());

		var resultado = service.sincronizar(1L, null, null);

		assertThat(resultado.naoVinculados()).isEqualTo(1);
		assertThat(resultado.ingeridos()).isZero();
		verify(gpsIngestaoService, never()).ingerir(any(), any(Equipamento.class), any());
	}

	@Test
	void distanciaZeroEMenorQueLimiarDeMovimento() {
		assertThat(RasSystemIngestaoService.metros(-8.0, -34.9, -8.0, -34.9)).isLessThan(1.0);
		assertThat(RasSystemIngestaoService.metros(-8.0, -34.9, -8.001, -34.9)).isGreaterThan(20.0);
	}

	private static TelemetriaGps telemetria(Long id, String lut, double lat, double lng) {
		return new TelemetriaGps(
			FonteGps.RASSYSTEM,
			id,
			lut,
			lut,
			lat,
			lng,
			Instant.parse("2026-08-18T13:10:57Z"),
			8f,
			null,
			null,
			null,
			null,
			null,
			"DESLOCANDO",
			16,
			23,
			null,
			List.of(8018),
			null,
			null,
			Map.of()
		);
	}
}
