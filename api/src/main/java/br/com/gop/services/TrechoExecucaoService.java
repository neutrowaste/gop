package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Escala;
import br.com.gop.models.StatusEscala;
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
 * Plano diário de trechos+ordem dentro da Escala (RN-001). Só pode ser
 * editado enquanto a Escala está em RASCUNHO — depois de PUBLICADA, o motor
 * de cobertura passa a atualizar status/percentual, mas a composição do
 * plano (quais trechos, em que ordem) fica travada.
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
	}

	@Transactional(readOnly = true)
	public TrechoExecucao buscarEntidade(Long empresaId, Long escalaId, Long id) {
		return trechoExecucaoRepository.findByIdAndEscalaIdAndEmpresaId(id, escalaId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Trecho não encontrado no plano desta escala"));
	}

	private Escala escalaEditavel(Long empresaId, Long escalaId) {
		Escala escala = escalaService.buscarEntidade(empresaId, escalaId);
		if (escala.getStatus() != StatusEscala.RASCUNHO) {
			throw new RegraNegocioException(
				"O plano de trechos só pode ser editado enquanto a escala está em RASCUNHO"
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

	private TrechoExecucaoResponse paraResponse(TrechoExecucao execucao) {
		return new TrechoExecucaoResponse(
			execucao.getId(),
			execucao.getEscala().getId(),
			execucao.getTrecho().getId(),
			execucao.getTrecho().getCodigo(),
			execucao.getTrecho().getRua().getNome(),
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
