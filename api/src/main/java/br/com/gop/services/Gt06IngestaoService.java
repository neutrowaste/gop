package br.com.gop.services;

import br.com.gop.integration.gt06.Gt06Posicao;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.FonteGps;
import br.com.gop.models.TelemetriaGps;
import br.com.gop.repositories.EquipamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Liga o IMEI do quadro GT06 ao lutocar (código externo) e grava a posição
 * pelo mesmo contrato do restante do motor.
 */
@Service
@Profile("postgres")
public class Gt06IngestaoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(Gt06IngestaoService.class);

	private final EquipamentoRepository equipamentoRepository;
	private final EquipamentoService equipamentoService;
	private final GpsIngestaoService gpsIngestaoService;

	public Gt06IngestaoService(
		EquipamentoRepository equipamentoRepository,
		EquipamentoService equipamentoService,
		GpsIngestaoService gpsIngestaoService
	) {
		this.equipamentoRepository = equipamentoRepository;
		this.equipamentoService = equipamentoService;
		this.gpsIngestaoService = gpsIngestaoService;
	}

	@Transactional
	public void ingerir(String imei, Gt06Posicao posicao) {
		if (!posicao.valido()) {
			LOGGER.info(
				"GT06 com coordenada zerada, posição ignorada: imei={}, lat={}, lng={}",
				imei,
				posicao.latitude(),
				posicao.longitude()
			);
			return;
		}
		List<String> variantes = variantes(imei);
		if (variantes.isEmpty()) {
			return;
		}
		List<Equipamento> encontrados = equipamentoRepository.findByCodigoExternoIn(variantes).stream()
			.filter(Equipamento::isAtivo)
			.toList();
		if (encontrados.isEmpty()) {
			LOGGER.warn(
				"IMEI {} sem lutocar ativo. Cadastre esse IMEI no código externo do equipamento.",
				imei
			);
			return;
		}
		if (encontrados.size() > 1) {
			LOGGER.warn("IMEI {} encontrado em {} lutocares; posição ignorada", imei, encontrados.size());
			return;
		}

		Equipamento equipamento = encontrados.get(0);
		Empresa empresa = equipamento.getEmpresa();
		if (!empresa.isAtivo()) {
			LOGGER.warn("IMEI {} pertence à empresa inativa {}", imei, empresa.getId());
			return;
		}
		if (equipamento.getFonte() == null) {
			equipamentoService.vincularExterno(equipamento, imei, FonteGps.PROPRIO);
		}

		TelemetriaGps telemetria = new TelemetriaGps(
			FonteGps.PROPRIO,
			null,
			equipamento.getCodigo(),
			equipamento.getDescricao(),
			posicao.latitude(),
			posicao.longitude(),
			posicao.capturadoEm(),
			posicao.velocidadeKmh(),
			posicao.direcaoGraus(),
			null,
			null,
			posicao.satelites(),
			null,
			"GPS",
			null,
			null,
			null,
			null,
			"POSICAO",
			null,
			Map.of("protocolo", "GT06")
		);
		gpsIngestaoService.ingerir(empresa.getId(), equipamento, telemetria);
	}

	static List<String> variantes(String imei) {
		if (imei == null) {
			return List.of();
		}
		String digitos = imei.replaceAll("\\D", "");
		if (digitos.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> chaves = new LinkedHashSet<>();
		chaves.add(digitos);
		String semZeros = digitos.replaceFirst("^0+", "");
		if (!semZeros.isEmpty()) {
			chaves.add(semZeros);
		}
		if (digitos.length() == 15) {
			chaves.add("0" + digitos);
		}
		return new ArrayList<>(chaves);
	}
}
