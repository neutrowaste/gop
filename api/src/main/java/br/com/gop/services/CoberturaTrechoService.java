package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.Turno;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.SnapTrechoProjecao;
import br.com.gop.repositories.TrechoExecucaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Motor de regras da Varrição (RN-001). Recebe uma posição GPS já persistida
 * (ou prestes a ser persistida), resolve a Escala PUBLICADA correspondente,
 * faz o snap geoespacial no plano do dia (padrão técnico do legado
 * apimonitoragps: ST_DWithin + ST_LineLocatePoint) e aplica as regras
 * estritas do GOP: limiar de cobertura configurável, 3 posições válidas
 * (início/meio/fim) e validação de ordem do plano.
 */
@Service
@Profile("postgres")
public class CoberturaTrechoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoberturaTrechoService.class);

	private final ParametroEmpresaService parametroEmpresaService;
	private final EscalaRepository escalaRepository;
	private final TrechoExecucaoRepository trechoExecucaoRepository;
	private final OcorrenciaService ocorrenciaService;

	public CoberturaTrechoService(
		ParametroEmpresaService parametroEmpresaService,
		EscalaRepository escalaRepository,
		TrechoExecucaoRepository trechoExecucaoRepository,
		OcorrenciaService ocorrenciaService
	) {
		this.parametroEmpresaService = parametroEmpresaService;
		this.escalaRepository = escalaRepository;
		this.trechoExecucaoRepository = trechoExecucaoRepository;
		this.ocorrenciaService = ocorrenciaService;
	}

	/**
	 * Processa a posição no motor de regras, atualizando os campos de snap na
	 * própria {@code posicao} (chamador é responsável por persistir).
	 *
	 * @return true se uma Escala PUBLICADA foi encontrada para o dia/ativo (a
	 *         posição foi de fato avaliada pelo motor); false se só ficou salva
	 *         sem processamento (comportamento documentado do RN-001).
	 */
	@Transactional
	public boolean processar(Long empresaId, Empresa empresa, Equipamento equipamento, PosicaoGps posicao) {
		Optional<Escala> escalaOpt = resolverEscalaPublicada(empresaId, equipamento.getId(), posicao.getCapturadoEm());
		if (escalaOpt.isEmpty()) {
			return false;
		}
		Escala escala = escalaOpt.get();
		double bufferMetros = parametroEmpresaService.snapBufferMetros(empresaId);
		double longitude = posicao.getGeom().getX();
		double latitude = posicao.getGeom().getY();

		Optional<SnapTrechoProjecao> snap = trechoExecucaoRepository.encontrarTrechoMaisProximo(
			escala.getId(),
			longitude,
			latitude,
			bufferMetros
		);
		if (snap.isEmpty()) {
			posicao.setValido(false);
			return true;
		}

		SnapTrechoProjecao resultado = snap.get();
		TrechoExecucao execucao = trechoExecucaoRepository.findById(resultado.getId())
			.orElseThrow(() -> new IllegalStateException("TrechoExecucao do snap não encontrado: " + resultado.getId()));

		posicao.setTrechoExecucao(execucao);
		posicao.setDistanciaEixoMetros(
			resultado.getDistanciaMetros() != null ? resultado.getDistanciaMetros().floatValue() : null
		);
		posicao.setProgressoLinear(
			resultado.getProgressoLinear() != null ? resultado.getProgressoLinear().floatValue() : null
		);
		posicao.setValido(true);

		atualizarCobertura(empresaId, empresa, escala, execucao, posicao.getProgressoLinear());
		ocorrenciaService.resolverAutomaticamenteGpsSilencioso(escala.getId(), equipamento.getId());
		return true;
	}

	private void atualizarCobertura(
		Long empresaId,
		Empresa empresa,
		Escala escala,
		TrechoExecucao execucao,
		Float progressoLinear
	) {
		float progresso01 = progressoLinear == null ? 0f : Math.max(0f, Math.min(1f, progressoLinear));
		float progressoPct = progresso01 * 100f;
		if (progressoPct > execucao.getPercentualExecutado()) {
			execucao.setPercentualExecutado(progressoPct);
		}
		execucao.setQuantidadePosicoesValidas(execucao.getQuantidadePosicoesValidas() + 1);

		Instant agora = Instant.now();
		if (execucao.getPrimeiraPosicaoValidaEm() == null) {
			execucao.setPrimeiraPosicaoValidaEm(agora);
		}
		execucao.setUltimaPosicaoValidaEm(agora);

		// RN-001: pelo menos 1 posição válida em cada faixa do trecho (início/meio/fim).
		if (progresso01 < (1.0 / 3)) {
			execucao.setTevePosicaoInicio(true);
		} else if (progresso01 < (2.0 / 3)) {
			execucao.setTevePosicaoMeio(true);
		} else {
			execucao.setTevePosicaoFim(true);
		}

		double limiarPercentual = parametroEmpresaService.coberturaMinimaPercentual(empresaId);
		boolean cobriuTresFaixas = execucao.isTevePosicaoInicio()
			&& execucao.isTevePosicaoMeio()
			&& execucao.isTevePosicaoFim()
			&& execucao.getQuantidadePosicoesValidas() >= 3;

		StatusTrecho statusAnterior = execucao.getStatus();
		if (statusAnterior != StatusTrecho.EXECUTADO
			&& execucao.getPercentualExecutado() >= limiarPercentual
			&& cobriuTresFaixas) {
			execucao.setStatus(StatusTrecho.EXECUTADO);
			trechoExecucaoRepository.save(execucao);
			LOGGER.info(
				"Trecho concluído pelo motor (RN-001): execucaoId={}, escalaId={}, trechoId={}, percentual={}",
				execucao.getId(),
				escala.getId(),
				execucao.getTrecho().getId(),
				execucao.getPercentualExecutado()
			);
			validarOrdemDoPlano(empresa, escala, execucao);
			return;
		}
		if (statusAnterior == StatusTrecho.PLANEJADO) {
			execucao.setStatus(StatusTrecho.PENDENTE);
		}
		trechoExecucaoRepository.save(execucao);
	}

	/**
	 * RN-001: "validar ordem crescente/decrescente do plano; se pulado, gera
	 * ocorrência". Interpretação adotada: a equipe pode varrer o plano em
	 * ordem crescente OU decrescente (ambos são progressões monotônicas
	 * válidas); só é considerado "pulo" quando nenhuma das duas direções está
	 * intacta, ou seja, existe trecho pendente tanto antes quanto depois do
	 * que acabou de ser concluído.
	 */
	private void validarOrdemDoPlano(Empresa empresa, Escala escala, TrechoExecucao concluido) {
		int ordem = concluido.getOrdem();
		List<TrechoExecucao> antes = trechoExecucaoRepository.findByEscalaIdAndOrdemLessThanAndAtivoTrue(
			escala.getId(),
			ordem
		);
		List<TrechoExecucao> depois = trechoExecucaoRepository.findByEscalaIdAndOrdemGreaterThanAndAtivoTrue(
			escala.getId(),
			ordem
		);
		boolean todosAntesConcluidos = antes.stream().allMatch(CoberturaTrechoService::estaConcluido);
		boolean todosDepoisConcluidos = depois.stream().allMatch(CoberturaTrechoService::estaConcluido);
		if (!todosAntesConcluidos && !todosDepoisConcluidos) {
			ocorrenciaService.criarOrdemViolada(empresa, escala, concluido);
		}
	}

	private static boolean estaConcluido(TrechoExecucao execucao) {
		StatusTrecho status = execucao.getStatus();
		return status == StatusTrecho.EXECUTADO || status == StatusTrecho.JUSTIFICADO || status == StatusTrecho.FORA_DA_ROTA;
	}

	/**
	 * Resolve a Escala PUBLICADA do dia para o ativo GPS. Caso exista mais de
	 * uma (mesmo equipamento em turnos diferentes no mesmo dia), desempata
	 * pela janela de horário fixa do turno; caso nenhuma bata, usa a primeira.
	 */
	private Optional<Escala> resolverEscalaPublicada(Long empresaId, Long equipamentoId, Instant capturadoEm) {
		LocalDate data = capturadoEm.atZone(ZoneOffset.UTC).toLocalDate();
		List<Escala> escalas = escalaRepository.findByEmpresaIdAndEquipamentoIdAndDataAndStatusAndAtivoTrue(
			empresaId,
			equipamentoId,
			data,
			StatusEscala.PUBLICADA
		);
		if (escalas.isEmpty()) {
			return Optional.empty();
		}
		if (escalas.size() == 1) {
			return Optional.of(escalas.get(0));
		}
		LocalTime hora = capturadoEm.atZone(ZoneOffset.UTC).toLocalTime();
		return escalas.stream()
			.filter(escala -> turnoContemHorario(escala.getTurno(), hora))
			.findFirst()
			.or(() -> Optional.of(escalas.get(0)));
	}

	/** Janelas fixas nesta fase (sem UI de configuração por turno ainda). */
	private boolean turnoContemHorario(Turno turno, LocalTime hora) {
		return switch (turno) {
			case MANHA -> !hora.isBefore(LocalTime.of(6, 0)) && hora.isBefore(LocalTime.of(12, 0));
			case TARDE -> !hora.isBefore(LocalTime.of(12, 0)) && hora.isBefore(LocalTime.of(18, 0));
			case NOITE -> !hora.isBefore(LocalTime.of(18, 0)) || hora.isBefore(LocalTime.of(6, 0));
		};
	}
}
