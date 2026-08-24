package br.com.gop.services;

import br.com.gop.models.Colaborador;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.Setor;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.Turno;
import br.com.gop.models.Usuario;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.PosicaoGpsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RN-001: silêncio de transmissão >= limiar configurado (padrão 60min) abre Ocorrência idempotente. */
@ExtendWith(MockitoExtension.class)
class SaudeGpsSchedulerTests {

	@Mock
	private EscalaRepository escalaRepository;

	@Mock
	private PosicaoGpsRepository posicaoGpsRepository;

	@Mock
	private ParametroEmpresaService parametroEmpresaService;

	@Mock
	private OcorrenciaService ocorrenciaService;

	private SaudeGpsScheduler scheduler;

	@BeforeEach
	void configurar() {
		scheduler = new SaudeGpsScheduler(escalaRepository, posicaoGpsRepository, parametroEmpresaService, ocorrenciaService);
	}

	@Test
	void abreOcorrenciaQuandoSilencioUltrapassaOLimiteConfigurado() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa);

		when(escalaRepository.findByStatusAndDataAndAtivoTrue(eq(StatusEscala.PUBLICADA), any(LocalDate.class)))
			.thenReturn(List.of(escala));
		when(parametroEmpresaService.silencioGpsMinutos(1L)).thenReturn(60L);

		PosicaoGps ultimaPosicao = new PosicaoGps();
		ultimaPosicao.setRecebidoEm(Instant.now().minus(90, ChronoUnit.MINUTES));
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(1L, 1L))
			.thenReturn(Optional.of(ultimaPosicao));

		scheduler.verificarSilencioGps();

		verify(ocorrenciaService).criarGpsSilencioso(
			eq(empresa),
			eq(escala),
			eq(equipamentoDa(escala)),
			org.mockito.ArgumentMatchers.longThat(minutos -> minutos >= 89)
		);
	}

	@Test
	void naoAbreOcorrenciaQuandoSilencioAindaDentroDoLimite() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa);

		when(escalaRepository.findByStatusAndDataAndAtivoTrue(eq(StatusEscala.PUBLICADA), any(LocalDate.class)))
			.thenReturn(List.of(escala));
		when(parametroEmpresaService.silencioGpsMinutos(1L)).thenReturn(60L);

		PosicaoGps ultimaPosicao = new PosicaoGps();
		ultimaPosicao.setRecebidoEm(Instant.now().minus(10, ChronoUnit.MINUTES));
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(1L, 1L))
			.thenReturn(Optional.of(ultimaPosicao));

		scheduler.verificarSilencioGps();

		verify(ocorrenciaService, never()).criarGpsSilencioso(any(), any(), any(), anyLong());
	}

	@Test
	void naoAbreOcorrenciaQuandoNaoHaEscalaPublicadaHoje() {
		when(escalaRepository.findByStatusAndDataAndAtivoTrue(eq(StatusEscala.PUBLICADA), any(LocalDate.class)))
			.thenReturn(List.of());

		scheduler.verificarSilencioGps();

		verify(ocorrenciaService, never()).criarGpsSilencioso(any(), any(), any(), anyLong());
	}

	private Empresa empresa(Long id) {
		Empresa empresa = new Empresa();
		empresa.setId(id);
		empresa.setCodigo("EMP");
		empresa.setNome("Empresa Teste");
		empresa.setAtivo(true);
		return empresa;
	}

	private Escala escala(Long id, Empresa empresa) {
		Setor setor = new Setor();
		setor.setId(1L);
		setor.setNome("Setor 1");

		Equipamento equipamento = new Equipamento();
		equipamento.setId(1L);
		equipamento.setCodigo("LUT-0001");

		Colaborador colaborador = new Colaborador();
		colaborador.setId(1L);
		colaborador.setNome("João Silva");
		colaborador.setMatricula("M-001");
		colaborador.setEquipamento(equipamento);

		Usuario supervisor = new Usuario();
		supervisor.setId(1L);
		supervisor.setNome("Supervisor 1");

		Escala escala = new Escala();
		escala.setId(id);
		escala.setEmpresa(empresa);
		escala.setData(LocalDate.now());
		escala.setTurno(Turno.MANHA);
		escala.setSetor(setor);
		escala.getColaboradores().add(colaborador);
		escala.setSupervisor(supervisor);
		escala.setStatus(StatusEscala.PUBLICADA);
		escala.setPublicadaEm(Instant.now().minus(5, ChronoUnit.HOURS));
		escala.setAtivo(true);
		return escala;
	}

	private Equipamento equipamentoDa(Escala escala) {
		return escala.equipamentosDosColaboradores().iterator().next();
	}
}
