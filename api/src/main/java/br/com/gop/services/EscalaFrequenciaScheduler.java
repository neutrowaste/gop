package br.com.gop.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Materializa a escala PUBLICADA do dia a partir dos modelos de frequência
 * ativos (RN-001). Idempotente: não recria se o dia já foi gerado.
 */
@Component
@Profile("postgres")
public class EscalaFrequenciaScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscalaFrequenciaScheduler.class);

	private final EscalaService escalaService;

	public EscalaFrequenciaScheduler(EscalaService escalaService) {
		this.escalaService = escalaService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void gerarNaSubida() {
		gerarDoDiaOperacional();
	}

	@Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
	public void gerarDiario() {
		gerarDoDiaOperacional();
	}

	void gerarDoDiaOperacional() {
		LocalDate hoje = LocalDate.now(EscalaService.FUSO_OPERACIONAL);
		int geradas = escalaService.gerarInstanciasDoDia(hoje);
		if (geradas > 0) {
			LOGGER.info("Frequências materializadas para {}: {} instância(s)", hoje, geradas);
		}
	}
}
