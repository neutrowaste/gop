package br.com.gop.services;

import br.com.gop.integration.rassystem.RasFilial;
import br.com.gop.integration.rassystem.RasRotulo;
import br.com.gop.integration.rassystem.RasSystemClient;
import br.com.gop.integration.rassystem.RasVeiculo;
import br.com.gop.models.RasSystemStatusResponse;
import br.com.gop.models.TelemetriaGps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Leitura da API RasSystem (ACL). Não persiste e não conhece o motor RN-001.
 */
@Service
public class RasSystemLeituraService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RasSystemLeituraService.class);

	private final RasSystemClient client;

	public RasSystemLeituraService(RasSystemClient client) {
		this.client = client;
	}

	public RasSystemStatusResponse status() {
		if (!client.isConfigurado()) {
			return new RasSystemStatusResponse(
				false,
				client.properties().getBaseUrl(),
				null,
				client.properties().getIdFilial(),
				client.properties().getIdRotulo(),
				false,
				0,
				"Integração RasSystem não configurada"
			);
		}
		try {
			List<RasFilial> filiais = client.listarFiliais();
			return new RasSystemStatusResponse(
				true,
				client.properties().getBaseUrl(),
				client.properties().getLogin(),
				client.properties().getIdFilial(),
				client.properties().getIdRotulo(),
				true,
				filiais.size(),
				null
			);
		} catch (RuntimeException exception) {
			LOGGER.warn("Falha ao pingar RasSystem: {}", exception.getMessage());
			return new RasSystemStatusResponse(
				true,
				client.properties().getBaseUrl(),
				client.properties().getLogin(),
				client.properties().getIdFilial(),
				client.properties().getIdRotulo(),
				false,
				0,
				exception.getMessage()
			);
		}
	}

	public List<RasFilial> filiais() {
		return client.listarFiliais();
	}

	public List<RasRotulo> rotulos(long idFilial) {
		return client.listarRotulos(idFilial);
	}

	public List<RasVeiculo> veiculos(long idRotulo) {
		return client.listarVeiculos(idRotulo);
	}

	public List<TelemetriaGps> ultimasCoordenadas(Integer idFilial, Integer idRotulo, Integer idTipoServico) {
		List<Integer> filiais = filiaisParaConsulta(idFilial);
		Integer rotulo = resolverRotulo(idRotulo);
		Integer tipo = resolverTipo(idTipoServico);
		List<TelemetriaGps> pontos = new ArrayList<>();
		List<String> falhas = new ArrayList<>();
		for (Integer filial : filiais) {
			try {
				pontos.addAll(client.ultimasCoordenadas(filial, rotulo, tipo));
			} catch (RuntimeException exception) {
				LOGGER.warn("Falha ao ler coordenadas da filial {}: {}", filial, exception.getMessage());
				falhas.add("filial " + filial + ": " + exception.getMessage());
			}
		}
		if (pontos.isEmpty() && !falhas.isEmpty()) {
			throw new IntegracaoExternaException(
				"Nenhuma filial da RasSystem retornou coordenadas. " + String.join("; ", falhas)
			);
		}
		return pontos;
	}

	public List<TelemetriaGps> rastreio(long idVeiculo, String dataInicio, String dataFim) {
		return client.rastreio(idVeiculo, dataInicio, dataFim);
	}

	public List<TelemetriaGps> eventos(long idVeiculo, String dataInicio, String dataFim) {
		return client.eventos(idVeiculo, dataInicio, dataFim);
	}

	Integer resolverFilial(Integer idFilial) {
		return filiaisParaConsulta(idFilial).get(0);
	}

	Integer resolverRotulo(Integer idRotulo) {
		return idRotulo != null ? idRotulo : client.properties().getIdRotulo();
	}

	Integer resolverTipo(Integer idTipoServico) {
		return idTipoServico != null ? idTipoServico : client.properties().getIdTipoServico();
	}

	List<Integer> filiaisParaConsulta(Integer idFilial) {
		if (idFilial != null) {
			return List.of(idFilial);
		}
		Integer configurada = client.properties().getIdFilial();
		if (configurada != null) {
			return List.of(configurada);
		}
		List<RasFilial> filiais = client.listarFiliais();
		List<Integer> ids = new ArrayList<>();
		for (RasFilial filial : filiais) {
			if (filial.idFilial() != null) {
				ids.add(filial.idFilial().intValue());
			}
		}
		if (ids.isEmpty()) {
			throw new RegraNegocioException(
				"A RasSystem não retornou nenhuma filial para esta conta. Confira o login ou informe RASSYSTEM_ID_FILIAL."
			);
		}
		LOGGER.info("RasSystem sem id-filial configurado; usando filiais {}", ids);
		return ids;
	}
}
