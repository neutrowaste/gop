package br.com.gop.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalaFrequenciaSchedulerTests {

	@Mock
	private EscalaService escalaService;

	private EscalaFrequenciaScheduler scheduler;

	@BeforeEach
	void configurar() {
		scheduler = new EscalaFrequenciaScheduler(escalaService);
	}

	@Test
	void materializaInstanciasDoDiaOperacional() {
		when(escalaService.gerarInstanciasDoDia(any(LocalDate.class))).thenReturn(2);

		scheduler.gerarDoDiaOperacional();

		verify(escalaService).gerarInstanciasDoDia(LocalDate.now(EscalaService.FUSO_OPERACIONAL));
	}
}
