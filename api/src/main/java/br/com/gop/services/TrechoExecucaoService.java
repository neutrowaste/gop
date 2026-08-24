package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Escala;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.TrechoExecucaoReordenarRequest;
import br.com.gop.models.TrechoExecucaoRequest;
import br.com.gop.models.TrechoExecucaoResponse;
import br.com.gop.repositories.TrechoExecucaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Plano de trechos da Escala (RN-001). Editável na avulsa e no modelo de
 * frequência enquanto a escala está ativa. Instância gerada pela frequência
 * só exibe o progresso do dia — a composição se altera no modelo.
 */
@Service
@Profile("postgres")
public class TrechoExecucaoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TrechoExecucaoService.class);

	private final EmpresaService empresaService;
	private final EscalaService escalaService;
	private final TrechoService trechoService;
	private final TrechoExecucaoRepository trechoExecucaoRepository;
	private final AuditoriaService auditoriaService;

	public TrechoExecucaoService(
		EmpresaService empresaService,
		EscalaService escalaService,
		TrechoService trechoService,
		TrechoExecucaoRepository trechoExecucaoRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.escalaService = escalaService;
		this.trechoService = trechoService;
		this.trechoExecucaoRepository = trechoExecucaoRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<TrechoExecucaoResponse> listar(Long empresaId, Long escalaId) {
		escalaService.buscarEntidade(empresaId, escalaId);
		return trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(escalaId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional
	public TrechoExecucaoResponse adicionar(Long empresaId, Long escalaId, TrechoExecucaoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = escalaEditavel(empresaId, escalaId);
		Trecho trecho = trechoService.buscarEntidade(empresaId, request.trechoId());
		if (!trecho.isAtivo()) {
			throw new RegraNegocioException("Trecho informado está inativo");
		}
		if (!trecho.getSetor().getId().equals(escala.getSetor().getId())) {
			throw new RegraNegocioException("Trecho não pertence ao setor desta escala");
		}
		if (trechoExecucaoRepository.existsByEscalaIdAndTrechoIdAndAtivoTrue(escalaId, trecho.getId())) {
			throw new ConflitoNegocioException("Este trecho já está no plano desta escala");
		}
		if (trechoExecucaoRepository.existsByEscalaIdAndOrdemAndAtivoTrue(escalaId, request.ordem())) {
			throw new ConflitoNegocioException("Já existe um trecho nesta posição de ordem");
		}

		TrechoExecucao execucao = new TrechoExecucao();
		execucao.setEmpresa(empresa);
		execucao.setEscala(escala);
		execucao.setTrecho(trecho);
		execucao.setOrdem(request.ordem());
		execucao.setStatus(StatusTrecho.PLANEJADO);
		trechoExecucaoRepository.save(execucao);

		auditoriaService.registrar(
			empresa,
			"TRECHO_EXECUCAO_ADICIONADO",
			"TrechoExecucao",
			execucao.getId(),
			Map.of("escalaId", escalaId, "trechoId", trecho.getId(), "ordem", request.ordem())
		);
		LOGGER.info(
			"Trecho adicionado ao plano: execucaoId={}, escalaId={}, trechoId={}, ordem={}",
			execucao.getId(),
			escalaId,
			trecho.getId(),
			request.ordem()
		);
		replicarSeModeloFrequencia(escala);
		return paraResponse(execucao);
	}

	@Transactional
	public TrechoExecucaoResponse atualizarOrdem(
		Long empresaId,
		Long escalaId,
		Long id,
		TrechoExecucaoReordenarRequest request
	) {
		empresaAtiva(empresaId);
		escalaEditavel(empresaId, escalaId);
		TrechoExecucao execucao = buscarEntidade(empresaId, escalaId, id);
		if (trechoExecucaoRepository.existsByEscalaIdAndOrdemAndAtivoTrueAndIdNot(escalaId, request.ordem(), id)) {
			throw new ConflitoNegocioException("Já existe um trecho nesta posição de ordem");
		}
		execucao.setOrdem(request.ordem());
		trechoExecucaoRepository.save(execucao);
		LOGGER.info("Ordem do trecho no plano atualizada: execucaoId={}, novaOrdem={}", id, request.ordem());
		replicarSeModeloFrequencia(execucao.getEscala());
		return paraResponse(execucao);
	}

	@Transactional
	public void remover(Long empresaId, Long escalaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		escalaEditavel(empresaId, escalaId);
		TrechoExecucao execucao = buscarEntidade(empresaId, escalaId, id);
		execucao.setAtivo(false);
		trechoExecucaoRepository.save(execucao);
		auditoriaService.registrar(
			empresa,
			"TRECHO_EXECUCAO_REMOVIDO",
			"TrechoExecucao",
			id,
			Map.of("escalaId", escalaId, "trechoId", execucao.getTrecho().getId())
		);
		LOGGER.info("Trecho removido do plano: execucaoId={}, escalaId={}", id, escalaId);
		replicarSeModeloFrequencia(execucao.getEscala());
	}

	@Transactional(readOnly = true)
	public TrechoExecucao buscarEntidade(Long empresaId, Long escalaId, Long id) {
		return trechoExecucaoRepository.findByIdAndEscalaIdAndEmpresaId(id, escalaId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Trecho não encontrado no plano desta escala"));
	}

	private void replicarSeModeloFrequencia(Escala escala) {
		if (escala.isModeloFrequencia()) {
			escalaService.replicarPlanoNaInstanciaDeHoje(escala);
		}
	}

	private Escala escalaEditavel(Long empresaId, Long escalaId) {
		Escala escala = escalaService.buscarEntidade(empresaId, escalaId);
		if (!escala.isAtivo()) {
			throw new RegraNegocioException("O plano de trechos não pode ser editado em escala cancelada");
		}
		if (escala.isInstanciaFrequencia()) {
			throw new RegraNegocioException(
				"O plano desta escala veio da frequência. Edite os trechos no modelo recorrente"
			);
		}
		return escala;
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private static String nomeRua(Trecho trecho) {
		return trecho.getNomeRua() == null ? "" : trecho.getNomeRua();
	}

	private TrechoExecucaoResponse paraResponse(TrechoExecucao execucao) {
		return new TrechoExecucaoResponse(
			execucao.getId(),
			execucao.getEscala().getId(),
			execucao.getTrecho().getId(),
			execucao.getTrecho().getCodigo(),
			nomeRua(execucao.getTrecho()),
			execucao.getOrdem(),
			execucao.getStatus(),
			execucao.getPercentualExecutado(),
			execucao.getQuantidadePosicoesValidas(),
			execucao.isTevePosicaoInicio(),
			execucao.isTevePosicaoMeio(),
			execucao.isTevePosicaoFim(),
			execucao.getPrimeiraPosicaoValidaEm(),
			execucao.getUltimaPosicaoValidaEm(),
			execucao.isAtivo()
		);
	}
}
