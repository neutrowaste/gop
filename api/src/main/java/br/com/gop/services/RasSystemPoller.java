package br.com.gop.services;

import br.com.gop.integration.rassystem.RasSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Poller periódico das últimas coordenadas RasSystem (mesmo intervalo mínimo
 * do legado: 120s). Só grava lutocares já cadastrados na empresa configurada.
 */
@Component
@Profile("postgres")
@ConditionalOnProperty(prefix = "gop.rassystem", name = "poller-enabled", havingValue = "true")
public class RasSystemPoller {

	private static final Logger LOGGER = LoggerFactory.getLogger(RasSystemPoller.class);

	private final RasSystemProperties properties;
	private final RasSystemIngestaoService ingestaoService;

	public RasSystemPoller(RasSystemProperties properties, RasSystemIngestaoService ingestaoService) {
		this.properties = properties;
		this.ingestaoService = ingestaoService;
	}

	@Scheduled(fixedDelayString = "${gop.rassystem.poll-interval-ms:120000}", initialDelay = 30_000)
	public void sincronizar() {
		if (!properties.isConfigurado()) {
			LOGGER.warn("Poller RasSystem ligado, mas login/senha não estão configurados");
			return;
		}
		if (properties.getEmpresaId() == null) {
			LOGGER.warn("Poller RasSystem ligado, mas gop.rassystem.empresa-id não está configurado");
			return;
		}
		try {
			var resultado = ingestaoService.sincronizar(
				properties.getEmpresaId(),
				properties.getIdFilial(),
				properties.getIdRotulo()
			);
			LOGGER.info(
				"Poller RasSystem: lidos={} ingeridos={} ignorados={} naoVinculados={}",
				resultado.lidos(),
				resultado.ingeridos(),
				resultado.ignorados(),
				resultado.naoVinculados()
			);
		} catch (RuntimeException exception) {
			LOGGER.error("Poller RasSystem falhou: {}", exception.getMessage());
		}
	}
}
