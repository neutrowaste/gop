package br.com.gop.services;

import br.com.gop.models.Escala;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.StatusEscala;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.PosicaoGpsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * RN-001: "silêncio de transmissão >= 1 hora caracteriza falha, com
 * tolerância de perda de sinal de 30 minutos". Sem fila/WebSocket nesta fase
 * (mesma postura do legado com QUEUE_ENABLED=false): job periódico varre as
 * escalas PUBLICADA do dia (todas as empresas) e abre Ocorrência idempotente
 * quando o ativo GPS ultrapassa o limiar configurado.
 *
 * Nota: {@code tolerancia_sinal_minutos} fica configurado e disponível via
 * ParametroEmpresaService para uso futuro (ex.: um estado de "atenção" antes
 * da falha); nesta fase o único limiar que efetivamente abre Ocorrência é
 * {@code silencio_gps_minutos}, seguindo literalmente a redação do RN-001.
 */
@Component
@Profile("postgres")
public class SaudeGpsScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaudeGpsScheduler.class);

	private final EscalaRepository escalaRepository;
	private final PosicaoGpsRepository posicaoGpsRepository;
	private final ParametroEmpresaService parametroEmpresaService;
	private final OcorrenciaService ocorrenciaService;

	public SaudeGpsScheduler(
		EscalaRepository escalaRepository,
		PosicaoGpsRepository posicaoGpsRepository,
		ParametroEmpresaService parametroEmpresaService,
		OcorrenciaService ocorrenciaService
	) {
		this.escalaRepository = escalaRepository;
		this.posicaoGpsRepository = posicaoGpsRepository;
		this.parametroEmpresaService = parametroEmpresaService;
		this.ocorrenciaService = ocorrenciaService;
	}

	@Scheduled(fixedRateString = "PT5M", initialDelayString = "PT1M")
	@Transactional
	public void verificarSilencioGps() {
		LocalDate hoje = LocalDate.now(ZoneOffset.UTC);
		List<Escala> escalasPublicadas = escalaRepository.findByStatusAndDataAndAtivoTrue(StatusEscala.PUBLICADA, hoje);
		if (escalasPublicadas.isEmpty()) {
			return;
		}
		Instant agora = Instant.now();
		int ocorrenciasAbertas = 0;
		for (Escala escala : escalasPublicadas) {
			Long empresaId = escala.getEmpresa().getId();
			long limiteMinutos = parametroEmpresaService.silencioGpsMinutos(empresaId);
			for (var equipamento : escala.equipamentosDosColaboradores()) {
				Long equipamentoId = equipamento.getId();

				Instant ultimaTransmissao = posicaoGpsRepository
					.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(empresaId, equipamentoId)
					.map(PosicaoGps::getRecebidoEm)
					.orElseGet(() -> escala.getPublicadaEm() != null ? escala.getPublicadaEm() : escala.getCriadoEm());
				if (ultimaTransmissao == null) {
					continue;
				}

				long minutosSemSinal = Duration.between(ultimaTransmissao, agora).toMinutes();
				if (minutosSemSinal >= limiteMinutos) {
					ocorrenciaService.criarGpsSilencioso(escala.getEmpresa(), escala, equipamento, minutosSemSinal);
					ocorrenciasAbertas++;
				}
			}
		}
		if (ocorrenciasAbertas > 0) {
			LOGGER.info("Verificação de saúde do GPS: {} escala(s) com possível silêncio detectado", ocorrenciasAbertas);
		}
	}
}
