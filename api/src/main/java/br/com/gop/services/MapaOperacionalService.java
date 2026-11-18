package br.com.gop.services;

import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.GeoJsonGeometry;
import br.com.gop.models.MapaAtivoResponse;
import br.com.gop.models.MapaDesvioResponse;
import br.com.gop.models.MapaOperacionalResponse;
import br.com.gop.models.MapaTrechoResponse;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.SituacaoSinal;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.Turno;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.PosicaoGpsRepository;
import br.com.gop.repositories.PosicaoTrajetoConsulta;
import br.com.gop.repositories.TrechoExecucaoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

		List<MapaDesvioResponse> desvios = desviosDoDia(
			empresaId,
			dataConsultada,
			escalasPublicadas,
			toleranciaMinutos
		);
		return new MapaOperacionalResponse(dataConsultada, agora, ativos, trechos, desvios);
	}

	/**
	 * Trajeto real só fora do eixo. No trecho, a faixa verde continua sendo o
	 * recorte da geometria prevista até o percentual já alcançado.
	 */
	private List<MapaDesvioResponse> desviosDoDia(
		Long empresaId,
		LocalDate data,
		List<Escala> escalas,
		long toleranciaMinutos
	) {
		Map<Long, List<Escala>> porEquipamento = new LinkedHashMap<>();
		for (Escala escala : escalas) {
			for (Equipamento equipamento : escala.equipamentosDosColaboradores()) {
				porEquipamento.computeIfAbsent(equipamento.getId(), id -> new ArrayList<>()).add(escala);
			}
		}
		if (porEquipamento.isEmpty()) {
			return List.of();
		}
		Instant inicio = data.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant fim = data.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<TrajetoDesvio.Ponto> pontos = new ArrayList<>();
		for (PosicaoTrajetoConsulta consulta : posicaoGpsRepository.listarTrajeto(
			empresaId,
			porEquipamento.keySet(),
			inicio,
			fim
		)) {
			if (consulta.geom() == null || consulta.capturadoEm() == null || consulta.equipamentoId() == null) {
				continue;
			}
			pontos.add(new TrajetoDesvio.Ponto(
				consulta.equipamentoId(),
				consulta.geom().getX(),
				consulta.geom().getY(),
				consulta.capturadoEm(),
				consulta.valido()
			));
		}
		List<MapaDesvioResponse> desvios = new ArrayList<>();
		for (TrajetoDesvio.Linha linha : TrajetoDesvio.linhasForaDoTrecho(pontos, Duration.ofMinutes(toleranciaMinutos))) {
			List<Escala> candidatas = porEquipamento.getOrDefault(linha.equipamentoId(), List.of());
			if (candidatas.isEmpty()) {
				continue;
			}
			Escala escala = escalaDoInstante(candidatas, linha.saidaEm());
			Equipamento equipamento = escala.equipamentosDosColaboradores().stream()
				.filter(item -> linha.equipamentoId() == item.getId())
				.findFirst()
				.orElse(null);
			if (equipamento == null) {
				continue;
			}
			desvios.add(new MapaDesvioResponse(
				escala.getId(),
				equipamento.getId(),
				equipamento.getCodigo(),
				escala.nomesColaboradores(),
				escala.getSetor().getId(),
				escala.getSetor().getNome(),
				new GeoJsonGeometry("LineString", linha.coordenadas())
			));
		}
		return desvios;
	}

	/** Mesmo desempate do motor: turno só separa quando o ativo tem mais de uma escala no dia. */
	private Escala escalaDoInstante(List<Escala> escalas, Instant instante) {
		if (escalas.size() == 1) {
			return escalas.get(0);
		}
		LocalTime hora = instante.atZone(ZoneOffset.UTC).toLocalTime();
		return escalas.stream()
			.filter(escala -> turnoContemHorario(escala.getTurno(), hora))
			.findFirst()
			.orElse(escalas.get(0));
	}

	private boolean turnoContemHorario(Turno turno, LocalTime hora) {
		return switch (turno) {
			case MANHA -> !hora.isBefore(LocalTime.of(6, 0)) && hora.isBefore(LocalTime.of(12, 0));
			case TARDE -> !hora.isBefore(LocalTime.of(12, 0)) && hora.isBefore(LocalTime.of(18, 0));
			case NOITE -> !hora.isBefore(LocalTime.of(18, 0)) || hora.isBefore(LocalTime.of(6, 0));
		};
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
