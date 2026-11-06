package br.com.gop.services;

import br.com.gop.integration.gt06.Gt06Posicao;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.FonteGps;
import br.com.gop.models.TelemetriaGps;
import br.com.gop.repositories.EquipamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Gt06IngestaoServiceTests {

	@Mock
	private EquipamentoRepository equipamentoRepository;

	@Mock
	private EquipamentoService equipamentoService;

	@Mock
	private GpsIngestaoService gpsIngestaoService;

	private Gt06IngestaoService service;

	@BeforeEach
	void configurar() {
		service = new Gt06IngestaoService(equipamentoRepository, equipamentoService, gpsIngestaoService);
	}

	@Test
	void imeiCasaComCodigoExternoEGravaPosicao() {
		Equipamento equipamento = equipamento("867123456789012");
		when(equipamentoRepository.findByCodigoExternoIn(any())).thenReturn(List.of(equipamento));

		service.ingerir("0867123456789012", posicao(true));

		ArgumentCaptor<TelemetriaGps> telemetria = ArgumentCaptor.forClass(TelemetriaGps.class);
		verify(gpsIngestaoService).ingerir(eq(7L), eq(equipamento), telemetria.capture());
		assertThat(telemetria.getValue().fonte()).isEqualTo(FonteGps.PROPRIO);
		assertThat(telemetria.getValue().latitude()).isEqualTo(-8.05);
		assertThat(telemetria.getValue().longitude()).isEqualTo(-34.88);
		verify(equipamentoService).vincularExterno(equipamento, "0867123456789012", FonteGps.PROPRIO);
	}

	@Test
	void semLutocarNaoGrava() {
		when(equipamentoRepository.findByCodigoExternoIn(any())).thenReturn(List.of());

		service.ingerir("867123456789012", posicao(true));

		verify(gpsIngestaoService, never()).ingerir(any(), any(), any());
	}

	@Test
	void semFixNaoGrava() {
		service.ingerir("867123456789012", posicao(false));

		verify(equipamentoRepository, never()).findByCodigoExternoIn(any());
		verify(gpsIngestaoService, never()).ingerir(any(), any(), any());
	}

	private static Equipamento equipamento(String codigoExterno) {
		Empresa empresa = new Empresa();
		empresa.setId(7L);
		empresa.setAtivo(true);
		Equipamento equipamento = new Equipamento();
		equipamento.setId(4L);
		equipamento.setCodigo("LUT-0001");
		equipamento.setCodigoExterno(codigoExterno);
		equipamento.setEmpresa(empresa);
		equipamento.setAtivo(true);
		return equipamento;
	}

	private static Gt06Posicao posicao(boolean valido) {
		return new Gt06Posicao(-8.05, -34.88, Instant.parse("2026-11-03T14:45:00Z"), 36f, 180f, 8, valido);
	}
}
