package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.Equipe;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.Rua;
import br.com.gop.models.Setor;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.Turno;
import br.com.gop.models.Usuario;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.SnapTrechoProjecao;
import br.com.gop.repositories.TrechoExecucaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras puras do motor de Varrição (RN-001): faixas início/meio/fim,
 * limiar de cobertura, condição de EXECUTADO e validação de ordem do plano.
 */
@ExtendWith(MockitoExtension.class)
class CoberturaTrechoServiceTests {

	@Mock
	private ParametroEmpresaService parametroEmpresaService;

	@Mock
	private EscalaRepository escalaRepository;

	@Mock
	private TrechoExecucaoRepository trechoExecucaoRepository;

	@Mock
	private OcorrenciaService ocorrenciaService;

	private CoberturaTrechoService service;

	@BeforeEach
	void configurar() {
		service = new CoberturaTrechoService(
			parametroEmpresaService,
			escalaRepository,
			trechoExecucaoRepository,
			ocorrenciaService
		);
	}

	@Test
	void naoProcessaQuandoNaoHaEscalaPublicadaParaODia() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa, StatusEscala.PUBLICADA);
		Equipamento equipamento = escala.getEquipamento();
		PosicaoGps posicao = posicao();

		when(escalaRepository.findByEmpresaIdAndEquipamentoIdAndDataAndStatusAndAtivoTrue(
			eq(1L), eq(equipamento.getId()), any(), eq(StatusEscala.PUBLICADA)
		)).thenReturn(List.of());

		boolean processado = service.processar(1L, empresa, equipamento, posicao);

		assertThat(processado).isFalse();
		assertThat(posicao.isValido()).isFalse();
		verifyNoSnapAttempt();
	}

	@Test
	void marcaPosicaoInvalidaQuandoForaDoBufferDoPlano() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa, StatusEscala.PUBLICADA);
		Equipamento equipamento = escala.getEquipamento();
		PosicaoGps posicao = posicao();

		when(escalaRepository.findByEmpresaIdAndEquipamentoIdAndDataAndStatusAndAtivoTrue(
			eq(1L), eq(equipamento.getId()), any(), eq(StatusEscala.PUBLICADA)
		)).thenReturn(List.of(escala));
		when(parametroEmpresaService.snapBufferMetros(1L)).thenReturn(25.0);
		when(trechoExecucaoRepository.encontrarTrechoMaisProximo(eq(10L), anyDouble(), anyDouble(), eq(25.0)))
			.thenReturn(Optional.empty());

		boolean processado = service.processar(1L, empresa, equipamento, posicao);

		assertThat(processado).isTrue();
		assertThat(posicao.isValido()).isFalse();
	}

	@Test
	void marcaTrechoComoExecutadoAoCobrirAsTresFaixasEAtingirLimiar() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa, StatusEscala.PUBLICADA);
		TrechoExecucao execucao = trechoExecucao(500L, escala, trecho(100L, "T-01"), 1);

		configurarEscalaEParametros(empresa, escala, 25.0, 80.0);

		processarPonto(escala, execucao, 0.10); // início
		processarPonto(escala, execucao, 0.50); // meio
		processarPonto(escala, execucao, 0.95); // fim, percentual final 95% >= 80%

		assertThat(execucao.getStatus()).isEqualTo(StatusTrecho.EXECUTADO);
		assertThat(execucao.getQuantidadePosicoesValidas()).isEqualTo(3);
		assertThat(execucao.isTevePosicaoInicio()).isTrue();
		assertThat(execucao.isTevePosicaoMeio()).isTrue();
		assertThat(execucao.isTevePosicaoFim()).isTrue();
	}

	@Test
	void naoConcluiTrechoQuandoFaltaUmaDasTresFaixas() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa, StatusEscala.PUBLICADA);
		TrechoExecucao execucao = trechoExecucao(500L, escala, trecho(100L, "T-01"), 1);

		configurarEscalaEParametros(empresa, escala, 25.0, 80.0);

		processarPonto(escala, execucao, 0.05); // início
		processarPonto(escala, execucao, 0.95); // fim — faltou a faixa do meio

		assertThat(execucao.getStatus()).isNotEqualTo(StatusTrecho.EXECUTADO);
		assertThat(execucao.isTevePosicaoMeio()).isFalse();
	}

	@Test
	void naoConcluiTrechoQuandoPercentualNaoAtingeOLimiarConfigurado() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa, StatusEscala.PUBLICADA);
		TrechoExecucao execucao = trechoExecucao(500L, escala, trecho(100L, "T-01"), 1);

		configurarEscalaEParametros(empresa, escala, 25.0, 80.0);

		processarPonto(escala, execucao, 0.10);
		processarPonto(escala, execucao, 0.40);
		processarPonto(escala, execucao, 0.60); // cobre as 3 faixas mas percentual máximo é só 60%

		assertThat(execucao.getStatus()).isNotEqualTo(StatusTrecho.EXECUTADO);
	}

	@Test
	void criaOcorrenciaDeOrdemVioladaQuandoConcluiForaDeSequencia() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa, StatusEscala.PUBLICADA);
		TrechoExecucao anterior = trechoExecucao(499L, escala, trecho(99L, "T-00"), 1);
		anterior.setStatus(StatusTrecho.PLANEJADO);
		TrechoExecucao atual = trechoExecucao(500L, escala, trecho(100L, "T-01"), 2);
		TrechoExecucao posterior = trechoExecucao(501L, escala, trecho(101L, "T-02"), 3);
		posterior.setStatus(StatusTrecho.PLANEJADO);

		configurarEscalaEParametros(empresa, escala, 25.0, 80.0);
		when(trechoExecucaoRepository.findByEscalaIdAndOrdemLessThanAndAtivoTrue(10L, 2))
			.thenReturn(List.of(anterior));
		when(trechoExecucaoRepository.findByEscalaIdAndOrdemGreaterThanAndAtivoTrue(10L, 2))
			.thenReturn(List.of(posterior));

		processarPonto(escala, atual, 0.10);
		processarPonto(escala, atual, 0.50);
		processarPonto(escala, atual, 0.95);

		assertThat(atual.getStatus()).isEqualTo(StatusTrecho.EXECUTADO);
		verify(ocorrenciaService).criarOrdemViolada(empresa, escala, atual);
	}

	@Test
	void naoCriaOcorrenciaQuandoConclusaoRespeitaOrdemCrescente() {
		Empresa empresa = empresa(1L);
		Escala escala = escala(10L, empresa, StatusEscala.PUBLICADA);
		TrechoExecucao anterior = trechoExecucao(499L, escala, trecho(99L, "T-00"), 1);
		anterior.setStatus(StatusTrecho.EXECUTADO);
		TrechoExecucao atual = trechoExecucao(500L, escala, trecho(100L, "T-01"), 2);
		TrechoExecucao posterior = trechoExecucao(501L, escala, trecho(101L, "T-02"), 3);
		posterior.setStatus(StatusTrecho.PLANEJADO);

		configurarEscalaEParametros(empresa, escala, 25.0, 80.0);
		when(trechoExecucaoRepository.findByEscalaIdAndOrdemLessThanAndAtivoTrue(10L, 2))
			.thenReturn(List.of(anterior));
		when(trechoExecucaoRepository.findByEscalaIdAndOrdemGreaterThanAndAtivoTrue(10L, 2))
			.thenReturn(List.of(posterior));

		processarPonto(escala, atual, 0.10);
		processarPonto(escala, atual, 0.50);
		processarPonto(escala, atual, 0.95);

		assertThat(atual.getStatus()).isEqualTo(StatusTrecho.EXECUTADO);
		verify(ocorrenciaService, never()).criarOrdemViolada(any(), any(), any());
	}

	private void configurarEscalaEParametros(Empresa empresa, Escala escala, double buffer, double limiar) {
		when(escalaRepository.findByEmpresaIdAndEquipamentoIdAndDataAndStatusAndAtivoTrue(
			eq(empresa.getId()), eq(escala.getEquipamento().getId()), any(), eq(StatusEscala.PUBLICADA)
		)).thenReturn(List.of(escala));
		when(parametroEmpresaService.snapBufferMetros(empresa.getId())).thenReturn(buffer);
		when(parametroEmpresaService.coberturaMinimaPercentual(empresa.getId())).thenReturn(limiar);
	}

	private void processarPonto(Escala escala, TrechoExecucao execucao, double progressoLinear) {
		SnapTrechoProjecao projecao = mock(SnapTrechoProjecao.class);
		when(projecao.getId()).thenReturn(execucao.getId());
		when(projecao.getDistanciaMetros()).thenReturn(3.0);
		when(projecao.getProgressoLinear()).thenReturn(progressoLinear);
		when(trechoExecucaoRepository.encontrarTrechoMaisProximo(eq(escala.getId()), anyDouble(), anyDouble(), anyDouble()))
			.thenReturn(Optional.of(projecao));
		when(trechoExecucaoRepository.findById(execucao.getId())).thenReturn(Optional.of(execucao));

		service.processar(escala.getEmpresa().getId(), escala.getEmpresa(), escala.getEquipamento(), posicao());
	}

	private void verifyNoSnapAttempt() {
		org.mockito.Mockito.verifyNoInteractions(trechoExecucaoRepository);
	}

	private Empresa empresa(Long id) {
		Empresa empresa = new Empresa();
		empresa.setId(id);
		empresa.setCodigo("EMP");
		empresa.setNome("Empresa Teste");
		empresa.setAtivo(true);
		return empresa;
	}

	private Escala escala(Long id, Empresa empresa, StatusEscala status) {
		Setor setor = new Setor();
		setor.setId(1L);
		setor.setNome("Setor 1");

		Equipe equipe = new Equipe();
		equipe.setId(1L);
		equipe.setNome("Equipe 1");

		Equipamento equipamento = new Equipamento();
		equipamento.setId(1L);
		equipamento.setCodigo("LUT-0001");
		equipamento.setEquipe(equipe);

		Usuario supervisor = new Usuario();
		supervisor.setId(1L);
		supervisor.setNome("Supervisor 1");

		Escala escala = new Escala();
		escala.setId(id);
		escala.setEmpresa(empresa);
		escala.setData(java.time.LocalDate.now());
		escala.setTurno(Turno.MANHA);
		escala.setSetor(setor);
		escala.setEquipe(equipe);
		escala.setEquipamento(equipamento);
		escala.setSupervisor(supervisor);
		escala.setStatus(status);
		escala.setAtivo(true);
		return escala;
	}

	private Trecho trecho(Long id, String codigo) {
		Rua rua = new Rua();
		rua.setId(1L);
		rua.setNome("Rua Teste");

		Trecho trecho = new Trecho();
		trecho.setId(id);
		trecho.setRua(rua);
		trecho.setCodigo(codigo);
		trecho.setAtivo(true);
		return trecho;
	}

	private TrechoExecucao trechoExecucao(Long id, Escala escala, Trecho trecho, int ordem) {
		TrechoExecucao execucao = new TrechoExecucao();
		execucao.setId(id);
		execucao.setEmpresa(escala.getEmpresa());
		execucao.setEscala(escala);
		execucao.setTrecho(trecho);
		execucao.setOrdem(ordem);
		execucao.setStatus(StatusTrecho.PLANEJADO);
		execucao.setAtivo(true);
		return execucao;
	}

	private PosicaoGps posicao() {
		PosicaoGps posicao = new PosicaoGps();
		posicao.setGeom(GeometriaWkt.pontoDe(-23.55, -46.63));
		posicao.setCapturadoEm(Instant.now());
		return posicao;
	}
}
