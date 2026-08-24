package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.FonteGps;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.PosicaoGpsRequest;
import br.com.gop.models.PosicaoGpsResponse;
import br.com.gop.models.TelemetriaGps;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.repositories.PosicaoGpsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Ingestão síncrona de posições GPS (RN-001, fase 2: sem fila/WebSocket).
 * Persiste sempre; delega ao {@link CoberturaTrechoService} a avaliação das
 * regras quando há Escala PUBLICADA para o dia/ativo.
 */
@Service
@Profile("postgres")
public class GpsIngestaoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GpsIngestaoService.class);

	private final EmpresaService empresaService;
	private final EquipamentoService equipamentoService;
	private final PosicaoGpsRepository posicaoGpsRepository;
	private final CoberturaTrechoService coberturaTrechoService;

	public GpsIngestaoService(
		EmpresaService empresaService,
		EquipamentoService equipamentoService,
		PosicaoGpsRepository posicaoGpsRepository,
		CoberturaTrechoService coberturaTrechoService
	) {
		this.empresaService = empresaService;
		this.equipamentoService = equipamentoService;
		this.posicaoGpsRepository = posicaoGpsRepository;
		this.coberturaTrechoService = coberturaTrechoService;
	}

	@Transactional
	public PosicaoGpsResponse ingerir(Long empresaId, PosicaoGpsRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipamento equipamento = equipamentoService.resolver(
			empresaId,
			request.equipamentoId(),
			request.equipamentoCodigo(),
			request.codigoExterno()
		);
		if (!equipamento.isAtivo()) {
			throw new RegraNegocioException("Equipamento (Ativo GPS) está inativo");
		}

		PosicaoGps posicao = new PosicaoGps();
		posicao.setEmpresa(empresa);
		posicao.setEquipamento(equipamento);
		posicao.setGeom(GeometriaWkt.pontoDe(request.latitude(), request.longitude()));
		posicao.setVelocidadeKmh(request.velocidadeKmh());
		posicao.setPrecisaoMetros(request.precisaoMetros());
		posicao.setDirecaoGraus(request.direcaoGraus());
		posicao.setAltitudeMetros(request.altitudeMetros());
		posicao.setOdometro(request.odometro());
		posicao.setSatelites(request.satelites());
		posicao.setRd(request.rd());
		posicao.setSituacao(request.situacao());
		posicao.setFonte(request.fonte() != null ? request.fonte() : FonteGps.MANUAL);
		posicao.setIdExterno(request.codigoExterno());
		posicao.setCapturadoEm(request.capturadoEm() != null ? request.capturadoEm() : Instant.now());
		posicao.setRecebidoEm(Instant.now());

		boolean escalaEncontrada = coberturaTrechoService.processar(empresaId, empresa, equipamento, posicao);
		posicaoGpsRepository.save(posicao);

		LOGGER.info(
			"Posição GPS recebida: empresaId={}, equipamentoId={}, fonte={}, escalaEncontrada={}, valido={}",
			empresaId,
			equipamento.getId(),
			posicao.getFonte(),
			escalaEncontrada,
			posicao.isValido()
		);
		return paraResponse(posicao, escalaEncontrada);
	}

	@Transactional
	public PosicaoGpsResponse ingerir(Long empresaId, Equipamento equipamento, TelemetriaGps telemetria) {
		return ingerir(empresaId, deTelemetria(equipamento, telemetria));
	}

	public static PosicaoGpsRequest deTelemetria(Equipamento equipamento, TelemetriaGps telemetria) {
		String idExterno = telemetria.idExterno() == null ? null : String.valueOf(telemetria.idExterno());
		return new PosicaoGpsRequest(
			equipamento.getId(),
			equipamento.getCodigo(),
			idExterno,
			telemetria.latitude(),
			telemetria.longitude(),
			telemetria.velocidadeKmh(),
			null,
			telemetria.capturadoEm(),
			telemetria.direcaoGraus(),
			telemetria.altitudeMetros(),
			telemetria.odometro(),
			telemetria.satelites(),
			telemetria.rd(),
			telemetria.situacao(),
			telemetria.fonte() != null ? telemetria.fonte() : FonteGps.PROPRIO
		);
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private PosicaoGpsResponse paraResponse(PosicaoGps posicao, boolean escalaEncontrada) {
		TrechoExecucao execucao = posicao.getTrechoExecucao();
		return new PosicaoGpsResponse(
			posicao.getId(),
			posicao.getEquipamento().getId(),
			posicao.getEquipamento().getCodigo(),
			posicao.getGeom().getY(),
			posicao.getGeom().getX(),
			posicao.getVelocidadeKmh(),
			posicao.getPrecisaoMetros(),
			posicao.getCapturadoEm(),
			posicao.getRecebidoEm(),
			execucao != null ? execucao.getId() : null,
			execucao != null ? execucao.getTrecho().getCodigo() : null,
			posicao.getDistanciaEixoMetros(),
			posicao.getProgressoLinear(),
			posicao.isValido(),
			escalaEncontrada,
			posicao.getDirecaoGraus(),
			posicao.getAltitudeMetros(),
			posicao.getOdometro(),
			posicao.getSatelites(),
			posicao.getRd(),
			posicao.getSituacao(),
			posicao.getFonte()
		);
	}
}
