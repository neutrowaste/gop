package br.com.gop.services;

import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.MapaAtivoResponse;
import br.com.gop.models.MapaOperacionalResponse;
import br.com.gop.models.MapaTrechoResponse;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.SituacaoSinal;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.PosicaoGpsRepository;
import br.com.gop.repositories.TrechoExecucaoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrato agregado do mapa operacional (RN-001): última posição dos ativos e
 * plano de trechos (com status/percentual) de todas as Escalas PUBLICADA do
 * dia de uma empresa. Sem fila/WebSocket nesta fase (mesma postura do resto
 * do motor): o frontend atualiza este retrato por polling.
 */
@Service
@Profile("postgres")
public class MapaOperacionalService {

	private final EscalaRepository escalaRepository;
	private final TrechoExecucaoRepository trechoExecucaoRepository;
	private final PosicaoGpsRepository posicaoGpsRepository;
	private final ParametroEmpresaService parametroEmpresaService;

	public MapaOperacionalService(
		EscalaRepository escalaRepository,
		TrechoExecucaoRepository trechoExecucaoRepository,
		PosicaoGpsRepository posicaoGpsRepository,
		ParametroEmpresaService parametroEmpresaService
	) {
		this.escalaRepository = escalaRepository;
		this.trechoExecucaoRepository = trechoExecucaoRepository;
		this.posicaoGpsRepository = posicaoGpsRepository;
		this.parametroEmpresaService = parametroEmpresaService;
	}

	@Transactional(readOnly = true)
	public MapaOperacionalResponse obter(Long empresaId, LocalDate data) {
		LocalDate dataConsultada = data != null ? data : LocalDate.now(ZoneOffset.UTC);
		List<Escala> escalasPublicadas = escalaRepository
			.findByEmpresaIdAndDataAndStatusAndAtivoTrue(empresaId, dataConsultada, StatusEscala.PUBLICADA);

		long toleranciaMinutos = parametroEmpresaService.toleranciaSinalMinutos(empresaId);
		long silencioMinutos = parametroEmpresaService.silencioGpsMinutos(empresaId);
		Instant agora = Instant.now();

		List<MapaAtivoResponse> ativos = new ArrayList<>();
		List<MapaTrechoResponse> trechos = new ArrayList<>();

		for (Escala escala : escalasPublicadas) {
			for (Equipamento equipamento : escala.equipamentosDosColaboradores()) {
				posicaoGpsRepository
					.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(empresaId, equipamento.getId())
					.ifPresent(posicao -> ativos.add(
						paraAtivo(escala, equipamento, posicao, agora, toleranciaMinutos, silencioMinutos)
					));
			}

			for (TrechoExecucao trechoExecucao : trechoExecucaoRepository
				.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(escala.getId())) {
				trechos.add(paraTrecho(escala, trechoExecucao));
			}
		}

		return new MapaOperacionalResponse(dataConsultada, agora, ativos, trechos);
	}

	private MapaAtivoResponse paraAtivo(
		Escala escala,
		Equipamento equipamento,
		PosicaoGps posicao,
		Instant agora,
		long toleranciaMinutos,
		long silencioMinutos
	) {
		long minutosSemSinal = Duration.between(posicao.getRecebidoEm(), agora).toMinutes();
		SituacaoSinal situacaoSinal;
		if (minutosSemSinal >= silencioMinutos) {
			situacaoSinal = SituacaoSinal.OFFLINE;
		} else if (minutosSemSinal >= toleranciaMinutos) {
			situacaoSinal = SituacaoSinal.ATENCAO;
		} else {
			situacaoSinal = SituacaoSinal.ONLINE;
		}

		return new MapaAtivoResponse(
			escala.getId(),
			equipamento.getId(),
			equipamento.getCodigo(),
			escala.nomesColaboradores(),
			escala.getSetor().getId(),
			escala.getSetor().getNome(),
			escala.getTurno(),
			escala.getSupervisor().getId(),
			escala.getSupervisor().getNome(),
			posicao.getGeom().getY(),
			posicao.getGeom().getX(),
			posicao.getVelocidadeKmh(),
			posicao.getRecebidoEm(),
			situacaoSinal
		);
	}

	private MapaTrechoResponse paraTrecho(Escala escala, TrechoExecucao trechoExecucao) {
		var trecho = trechoExecucao.getTrecho();
		return new MapaTrechoResponse(
			trechoExecucao.getId(),
			escala.getId(),
			trecho.getId(),
			trecho.getCodigo(),
			trecho.getNomeRua() == null ? "" : trecho.getNomeRua(),
			escala.getSetor().getId(),
			escala.getSetor().getNome(),
			escala.nomesColaboradores(),
			trechoExecucao.getOrdem(),
			trechoExecucao.getStatus(),
			trechoExecucao.getPercentualExecutado(),
			GeometriaWkt.paraGeoJson(trecho.getGeom())
		);
	}
}
