package br.com.gop.services;

import br.com.gop.integration.rassystem.RasFilial;
import br.com.gop.integration.rassystem.RasRotulo;
import br.com.gop.integration.rassystem.RasSystemClient;
import br.com.gop.integration.rassystem.RasSystemLutocarMatcher;
import br.com.gop.integration.rassystem.RasVeiculo;
import br.com.gop.models.Equipamento;
import br.com.gop.models.EquipamentoRequest;
import br.com.gop.models.EquipamentoResponse;
import br.com.gop.models.FonteGps;
import br.com.gop.models.RasSystemImportarCadastroItemResponse;
import br.com.gop.models.RasSystemImportarCadastroResponse;
import br.com.gop.models.RasSystemImportarRastreioRequest;
import br.com.gop.models.RasSystemImportarRastreioResponse;
import br.com.gop.models.RasSystemLutocarResponse;
import br.com.gop.models.RasSystemSyncItemResponse;
import br.com.gop.models.RasSystemSyncResponse;
import br.com.gop.models.TelemetriaGps;
import br.com.gop.repositories.PosicaoGpsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Aplica telemetria RasSystem no modelo interno via {@link GpsIngestaoService}.
 * Não cria Equipamento (teto de ativos); apenas vincula LUT já cadastrado.
 */
@Service
@Profile("postgres")
public class RasSystemIngestaoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RasSystemIngestaoService.class);
	private static final double MIN_MOVE_METROS = 20.0;

	private final RasSystemClient client;
	private final RasSystemLeituraService leituraService;
	private final EquipamentoService equipamentoService;
	private final GpsIngestaoService gpsIngestaoService;
	private final PosicaoGpsRepository posicaoGpsRepository;

	public RasSystemIngestaoService(
		RasSystemClient client,
		RasSystemLeituraService leituraService,
		EquipamentoService equipamentoService,
		GpsIngestaoService gpsIngestaoService,
		PosicaoGpsRepository posicaoGpsRepository
	) {
		this.client = client;
		this.leituraService = leituraService;
		this.equipamentoService = equipamentoService;
		this.gpsIngestaoService = gpsIngestaoService;
		this.posicaoGpsRepository = posicaoGpsRepository;
	}

	public List<RasSystemLutocarResponse> listarLutocares(
		Long empresaId,
		Integer idFilial,
		Integer idRotulo
	) {
		Integer rotulo = leituraService.resolverRotulo(idRotulo);
		List<Equipamento> equipamentos = equipamentoService.listarEntidades(empresaId);
		Map<Long, TelemetriaGps> porVeiculo = new HashMap<>();
		for (TelemetriaGps ponto : leituraService.ultimasCoordenadas(idFilial, rotulo, null)) {
			if (ponto.idExterno() != null) {
				porVeiculo.put(ponto.idExterno(), ponto);
			}
		}

		List<RasSystemLutocarResponse> resultado = new ArrayList<>();
		if (rotulo != null) {
			for (RasVeiculo veiculo : client.listarVeiculos(rotulo)) {
				TelemetriaGps ultima = porVeiculo.remove(veiculo.id());
				resultado.add(paraLutocar(equipamentos, veiculo, ultima));
			}
		}
		for (TelemetriaGps restante : porVeiculo.values()) {
			resultado.add(paraLutocar(equipamentos, null, restante));
		}
		return resultado;
	}

	/**
	 * Cadastra até {@code limite} lutocares da RasSystem ainda não vinculados,
	 * priorizando quem está transmitindo, e em seguida sincroniza posições.
	 */
	public RasSystemImportarCadastroResponse importarCadastro(
		Long empresaId,
		Integer limite,
		Integer idFilial,
		Integer idRotulo
	) {
		int alvo = limite == null ? 5 : limite;
		if (alvo < 1 || alvo > 20) {
			throw new RegraNegocioException("Informe um limite entre 1 e 20 lutocares");
		}

		List<RasSystemLutocarResponse> candidatos = listarLutocares(empresaId, idFilial, idRotulo).stream()
			.filter(item -> item.idVeiculo() != null)
			.filter(item -> !item.vinculado())
			.sorted(Comparator
				.comparing((RasSystemLutocarResponse item) -> item.ultimaPosicao() == null)
				.thenComparing(item -> codigoCadastro(item) == null))
			.limit(alvo)
			.toList();

		List<RasSystemImportarCadastroItemResponse> itens = new ArrayList<>();
		int criados = 0;
		int jaExistentes = 0;
		int ignorados = 0;
		boolean tetoAtingido = false;

		for (RasSystemLutocarResponse lutocar : candidatos) {
			String codigo = codigoCadastro(lutocar);
			if (tetoAtingido) {
				ignorados++;
				itens.add(itemCadastro(lutocar, codigo, "IGNORADO", null, "Teto de Lutocares atingido"));
				continue;
			}
			if (codigo == null) {
				ignorados++;
				itens.add(itemCadastro(lutocar, null, "SEM_CODIGO", null, "Sem código LUT nem idVeiculo"));
				continue;
			}
			String externo = String.valueOf(lutocar.idVeiculo());
			String descricao = primeiroNaoVazio(lutocar.descricao(), lutocar.placa());
			try {
				EquipamentoResponse criado = equipamentoService.criar(
					empresaId,
					new EquipamentoRequest(codigo, descricao, externo, true, FonteGps.RASSYSTEM)
				);
				criados++;
				itens.add(itemCadastro(lutocar, codigo, "CRIADO", criado.id(), null));
			} catch (ConflitoNegocioException exception) {
				jaExistentes++;
				Long equipamentoId = vincularExistente(empresaId, codigo, externo);
				itens.add(itemCadastro(lutocar, codigo, "JA_EXISTENTE", equipamentoId, exception.getMessage()));
			} catch (RegraNegocioException exception) {
				tetoAtingido = exception.getMessage() != null
					&& exception.getMessage().toLowerCase(Locale.ROOT).contains("teto");
				ignorados++;
				itens.add(itemCadastro(
					lutocar,
					codigo,
					tetoAtingido ? "TETO" : "IGNORADO",
					null,
					exception.getMessage()
				));
			}
		}

		LOGGER.info(
			"Importação RasSystem: empresaId={} alvo={} criados={} jaExistentes={} ignorados={}",
			empresaId,
			alvo,
			criados,
			jaExistentes,
			ignorados
		);
		RasSystemSyncResponse sync = sincronizar(empresaId, idFilial, idRotulo);
		return new RasSystemImportarCadastroResponse(alvo, criados, jaExistentes, ignorados, itens, sync);
	}

	public RasSystemSyncResponse sincronizar(Long empresaId, Integer idFilial, Integer idRotulo) {
		List<TelemetriaGps> pontos = leituraService.ultimasCoordenadas(idFilial, idRotulo, null);
		List<Equipamento> equipamentos = equipamentoService.listarEntidades(empresaId);
		List<RasSystemSyncItemResponse> itens = new ArrayList<>();
		int ingeridos = 0;
		int ignorados = 0;
		int naoVinculados = 0;

		for (TelemetriaGps ponto : pontos) {
			Optional<Equipamento> vinculado = RasSystemLutocarMatcher.vincular(
				equipamentos,
				ponto.idExterno(),
				ponto.codigoLogico(),
				ponto.rotuloVeiculo()
			);
			if (vinculado.isEmpty()) {
				naoVinculados++;
				itens.add(item(ponto, "NAO_VINCULADO", null, "Lutocar não cadastrado no GOP"));
				continue;
			}
			Equipamento equipamento = vinculado.get();
			if (!equipamento.isAtivo()) {
				ignorados++;
				itens.add(item(ponto, "INATIVO", equipamento, "Equipamento inativo"));
				continue;
			}
			String motivo = motivoIgnorar(empresaId, equipamento, ponto);
			if (motivo != null) {
				ignorados++;
				itens.add(item(ponto, "IGNORADO", equipamento, motivo));
				continue;
			}
			equipamentoService.vincularExterno(
				equipamento,
				ponto.idExterno() == null ? null : String.valueOf(ponto.idExterno()),
				FonteGps.RASSYSTEM
			);
			gpsIngestaoService.ingerir(empresaId, equipamento, ponto);
			ingeridos++;
			itens.add(item(ponto, "INGERIDO", equipamento, null));
		}

		LOGGER.info(
			"Sync RasSystem: empresaId={} lidos={} ingeridos={} ignorados={} naoVinculados={}",
			empresaId,
			pontos.size(),
			ingeridos,
			ignorados,
			naoVinculados
		);
		return new RasSystemSyncResponse(pontos.size(), ingeridos, ignorados, naoVinculados, itens);
	}

	public RasSystemImportarRastreioResponse importar(
		Long empresaId,
		RasSystemImportarRastreioRequest request
	) {
		RasVeiculo veiculo = resolverVeiculo(request.veiculo(), leituraService.resolverFilial(request.idFilial()));
		List<TelemetriaGps> pontos = client.rastreio(veiculo.id(), request.dataInicio(), request.dataFim());
		List<TelemetriaGps> eventos = client.eventos(veiculo.id(), request.dataInicio(), request.dataFim());
		List<Equipamento> equipamentos = equipamentoService.listarEntidades(empresaId);
		Optional<Equipamento> vinculado = RasSystemLutocarMatcher.vincular(
			equipamentos,
			veiculo.id(),
			veiculo.descricao(),
			veiculo.placa()
		);

		int ingeridos = 0;
		int ignorados = 0;
		boolean persistir = request.persistir() == null || request.persistir();
		if (persistir && vinculado.isPresent() && vinculado.get().isAtivo()) {
			Equipamento equipamento = vinculado.get();
			equipamentoService.vincularExterno(equipamento, String.valueOf(veiculo.id()), FonteGps.RASSYSTEM);
			for (TelemetriaGps ponto : pontos) {
				String motivo = motivoIgnorar(empresaId, equipamento, ponto);
				if (motivo != null) {
					ignorados++;
					continue;
				}
				gpsIngestaoService.ingerir(empresaId, equipamento, ponto);
				ingeridos++;
			}
		} else if (persistir && vinculado.isEmpty()) {
			throw new RecursoNaoEncontradoException(
				"Lutocar \"" + request.veiculo() + "\" não está cadastrado no GOP; cadastre o código LUT antes de importar"
			);
		}

		return new RasSystemImportarRastreioResponse(
			veiculo.id(),
			primeiroNaoVazio(veiculo.descricao(), veiculo.placa()),
			pontos.size(),
			eventos.size(),
			ingeridos,
			ignorados,
			vinculado.isPresent(),
			vinculado.map(Equipamento::getId).orElse(null),
			pontos,
			eventos
		);
	}

	private RasVeiculo resolverVeiculo(String veiculo, Integer idFilial) {
		String needle = veiculo.strip();
		if (needle.chars().allMatch(Character::isDigit)) {
			if (idFilial == null) {
				throw new RegraNegocioException("idFilial é obrigatório quando o veículo é informado pelo id numérico");
			}
			return new RasVeiculo(Long.parseLong(needle), needle, needle);
		}
		String alvo = needle.toUpperCase(Locale.ROOT);
		List<Integer> filiais = new ArrayList<>();
		if (idFilial != null) {
			filiais.add(idFilial);
		} else {
			for (RasFilial filial : client.listarFiliais()) {
				filiais.add(filial.idFilial().intValue());
			}
		}
		for (Integer filial : filiais) {
			try {
				for (RasRotulo rotulo : client.listarRotulos(filial)) {
					for (RasVeiculo candidato : client.listarVeiculos(rotulo.id())) {
						if (casaVeiculo(candidato, alvo)) {
							return candidato;
						}
					}
				}
			} catch (IntegracaoExternaException exception) {
				LOGGER.warn("Falha ao listar rótulos da filial {}: {}", filial, exception.getMessage());
			}
		}
		throw new RecursoNaoEncontradoException("Veículo \"" + veiculo + "\" não encontrado na RasSystem");
	}

	private static boolean casaVeiculo(RasVeiculo candidato, String alvo) {
		String descricao = candidato.descricao() == null ? "" : candidato.descricao().toUpperCase(Locale.ROOT);
		String placa = candidato.placa() == null ? "" : candidato.placa().toUpperCase(Locale.ROOT);
		return descricao.equals(alvo) || placa.equals(alvo)
			|| descricao.contains(alvo) || placa.contains(alvo);
	}

	private RasSystemLutocarResponse paraLutocar(
		List<Equipamento> equipamentos,
		RasVeiculo veiculo,
		TelemetriaGps ultima
	) {
		Long idVeiculo = veiculo != null ? veiculo.id() : ultima != null ? ultima.idExterno() : null;
		String descricao = veiculo != null ? veiculo.descricao() : ultima != null ? ultima.rotuloVeiculo() : null;
		String placa = veiculo != null ? veiculo.placa() : null;
		String codigoLogico = RasSystemLutocarMatcher.codigoLogico(descricao, placa,
			ultima != null ? ultima.codigoLogico() : null);
		Optional<Equipamento> vinculado = RasSystemLutocarMatcher.vincular(
			equipamentos,
			idVeiculo,
			codigoLogico,
			descricao,
			placa
		);
		return new RasSystemLutocarResponse(
			idVeiculo,
			descricao,
			placa,
			codigoLogico,
			vinculado.map(Equipamento::getId).orElse(null),
			vinculado.map(Equipamento::getCodigo).orElse(null),
			vinculado.isPresent(),
			ultima
		);
	}

	private String motivoIgnorar(Long empresaId, Equipamento equipamento, TelemetriaGps ponto) {
		return posicaoGpsRepository
			.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(empresaId, equipamento.getId())
			.map(ultima -> {
				if (ultima.getCapturadoEm() != null && ponto.capturadoEm() != null
					&& !ponto.capturadoEm().isAfter(ultima.getCapturadoEm())) {
					return "Posição igual ou mais antiga que a já persistida";
				}
				if (ultima.getGeom() != null
					&& metros(ultima.getGeom().getY(), ultima.getGeom().getX(), ponto.latitude(), ponto.longitude())
					< MIN_MOVE_METROS) {
					return "Deslocamento menor que " + (int) MIN_MOVE_METROS + " m";
				}
				return null;
			})
			.orElse(null);
	}

	private Long vincularExistente(Long empresaId, String codigo, String externo) {
		try {
			Equipamento existente = equipamentoService.resolver(empresaId, null, codigo, null);
			equipamentoService.vincularExterno(existente, externo, FonteGps.RASSYSTEM);
			return existente.getId();
		} catch (RuntimeException exception) {
			LOGGER.warn("Não foi possível vincular lutocar existente {}: {}", codigo, exception.getMessage());
			return null;
		}
	}

	private static String codigoCadastro(RasSystemLutocarResponse lutocar) {
		String extraido = RasSystemLutocarMatcher.codigoLogico(
			lutocar.codigoLogico(),
			lutocar.descricao(),
			lutocar.placa()
		);
		if (extraido != null) {
			return extraido;
		}
		if (lutocar.codigoLogico() != null && !lutocar.codigoLogico().isBlank()) {
			return lutocar.codigoLogico().strip().toUpperCase(Locale.ROOT);
		}
		if (lutocar.idVeiculo() != null) {
			return "RAS-" + lutocar.idVeiculo();
		}
		return null;
	}

	private static RasSystemImportarCadastroItemResponse itemCadastro(
		RasSystemLutocarResponse lutocar,
		String codigo,
		String status,
		Long equipamentoId,
		String detalhe
	) {
		return new RasSystemImportarCadastroItemResponse(
			lutocar.idVeiculo(),
			codigo,
			status,
			equipamentoId,
			detalhe
		);
	}

	private static RasSystemSyncItemResponse item(
		TelemetriaGps ponto,
		String status,
		Equipamento equipamento,
		String detalhe
	) {
		return new RasSystemSyncItemResponse(
			ponto.idExterno(),
			ponto.codigoLogico(),
			ponto.rotuloVeiculo(),
			status,
			equipamento != null ? equipamento.getId() : null,
			detalhe
		);
	}

	private static String primeiroNaoVazio(String... valores) {
		for (String valor : valores) {
			if (valor != null && !valor.isBlank()) {
				return valor;
			}
		}
		return null;
	}

	static double metros(double lat1, double lon1, double lat2, double lon2) {
		double raio = 6_371_000.0;
		double p1 = Math.toRadians(lat1);
		double p2 = Math.toRadians(lat2);
		double dp = Math.toRadians(lat2 - lat1);
		double dl = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dp / 2) * Math.sin(dp / 2)
			+ Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
		return 2 * raio * Math.asin(Math.min(1.0, Math.sqrt(a)));
	}
}
