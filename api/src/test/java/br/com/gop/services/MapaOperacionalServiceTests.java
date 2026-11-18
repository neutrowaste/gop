package br.com.gop.services;

import br.com.gop.models.Colaborador;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.GeoJsonGeometry;
import br.com.gop.models.MapaOperacionalResponse;
import br.com.gop.models.PosicaoGps;
import br.com.gop.models.Setor;
import br.com.gop.models.SituacaoSinal;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.Turno;
import br.com.gop.models.Usuario;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.PosicaoGpsRepository;
import br.com.gop.repositories.PosicaoTrajetoConsulta;
import br.com.gop.repositories.TrechoExecucaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Retrato do mapa operacional (RN-001): ativos (última posição) e trechos
 * (plano + status) das Escalas PUBLICADA do dia de uma empresa.
 */
@ExtendWith(MockitoExtension.class)
class MapaOperacionalServiceTests {

	private static final Long EMPRESA_ID = 1L;
	private static final LocalDate HOJE = LocalDate.now();

	@Mock
	private EscalaRepository escalaRepository;

	@Mock
	private TrechoExecucaoRepository trechoExecucaoRepository;

	@Mock
	private PosicaoGpsRepository posicaoGpsRepository;

	@Mock
	private ParametroEmpresaService parametroEmpresaService;

	private MapaOperacionalService service;

	@BeforeEach
	void configurar() {
		service = new MapaOperacionalService(
			escalaRepository,
			trechoExecucaoRepository,
			posicaoGpsRepository,
			parametroEmpresaService
		);
		lenient().when(parametroEmpresaService.toleranciaSinalMinutos(EMPRESA_ID)).thenReturn(30L);
		lenient().when(parametroEmpresaService.silencioGpsMinutos(EMPRESA_ID)).thenReturn(60L);
		lenient().when(posicaoGpsRepository.listarTrajeto(any(), any(), any(), any())).thenReturn(List.of());
	}

	@Test
	void retornaListasVaziasQuandoNaoHaEscalaPublicadaNoDia() {
		when(escalaRepository.findByEmpresaIdAndDataAndStatusAndAtivoTrue(EMPRESA_ID, HOJE, StatusEscala.PUBLICADA))
			.thenReturn(List.of());

		MapaOperacionalResponse resposta = service.obter(EMPRESA_ID, HOJE);

		assertThat(resposta.ativos()).isEmpty();
		assertThat(resposta.trechos()).isEmpty();
		assertThat(resposta.desvios()).isEmpty();
		assertThat(resposta.data()).isEqualTo(HOJE);
	}

	@Test
	void omiteAtivoSemNenhumaPosicaoRegistrada() {
		Escala escala = escala(10L);
		when(escalaRepository.findByEmpresaIdAndDataAndStatusAndAtivoTrue(EMPRESA_ID, HOJE, StatusEscala.PUBLICADA))
			.thenReturn(List.of(escala));
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(
			EMPRESA_ID, equipamentoDa(escala).getId()
		)).thenReturn(Optional.empty());
		when(trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(escala.getId()))
			.thenReturn(List.of());

		MapaOperacionalResponse resposta = service.obter(EMPRESA_ID, HOJE);

		assertThat(resposta.ativos()).isEmpty();
	}

	@Test
	void incluiTrechoComGeometriaGeoJsonCorreta() {
		Escala escala = escala(10L);
		Trecho trecho = trecho(100L, "T-01", "LINESTRING(-46.63 -23.55, -46.64 -23.56)");
		TrechoExecucao execucao = trechoExecucao(500L, escala, trecho, 1, StatusTrecho.EXECUTADO, 95f);

		when(escalaRepository.findByEmpresaIdAndDataAndStatusAndAtivoTrue(EMPRESA_ID, HOJE, StatusEscala.PUBLICADA))
			.thenReturn(List.of(escala));
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(
			EMPRESA_ID, equipamentoDa(escala).getId()
		)).thenReturn(Optional.empty());
		when(trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(escala.getId()))
			.thenReturn(List.of(execucao));

		MapaOperacionalResponse resposta = service.obter(EMPRESA_ID, HOJE);

		assertThat(resposta.trechos()).hasSize(1);
		var trechoResposta = resposta.trechos().get(0);
		assertThat(trechoResposta.status()).isEqualTo(StatusTrecho.EXECUTADO);
		assertThat(trechoResposta.percentualExecutado()).isEqualTo(95f);
		GeoJsonGeometry geometria = trechoResposta.geometry();
		assertThat(geometria.type()).isEqualTo("LineString");
		double[][] coordenadas = (double[][]) geometria.coordinates();
		assertThat(coordenadas.length).isEqualTo(2);
		assertThat(coordenadas[0]).containsExactly(-46.63, -23.55);
	}

	@Test
	void desenhaDesvioSomenteQuandoOGpsSaiDoTrecho() {
		Escala escala = escala(10L);
		when(escalaRepository.findByEmpresaIdAndDataAndStatusAndAtivoTrue(EMPRESA_ID, HOJE, StatusEscala.PUBLICADA))
			.thenReturn(List.of(escala));
		when(trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(escala.getId()))
			.thenReturn(List.of());
		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(
			EMPRESA_ID, equipamentoDa(escala).getId()
		)).thenReturn(Optional.empty());
		Instant base = HOJE.atTime(9, 0).toInstant(ZoneOffset.UTC);
		when(posicaoGpsRepository.listarTrajeto(eq(EMPRESA_ID), any(), any(), any())).thenReturn(List.of(
			consulta(base, -23.550, -46.630, true),
			consulta(base.plus(1, ChronoUnit.MINUTES), -23.551, -46.631, false),
			consulta(base.plus(2, ChronoUnit.MINUTES), -23.552, -46.632, false),
			consulta(base.plus(3, ChronoUnit.MINUTES), -23.553, -46.630, true)
		));

		MapaOperacionalResponse resposta = service.obter(EMPRESA_ID, HOJE);

		assertThat(resposta.desvios()).hasSize(1);
		assertThat(resposta.desvios().get(0).equipamentoCodigo()).isEqualTo("LUT-0001");
		assertThat(resposta.desvios().get(0).escalaId()).isEqualTo(10L);
		assertThat(resposta.desvios().get(0).geometry().type()).isEqualTo("LineString");
		double[][] coordenadas = (double[][]) resposta.desvios().get(0).geometry().coordinates();
		assertThat(coordenadas).hasDimensions(4, 2);
		assertThat(coordenadas[0]).containsExactly(-46.630, -23.550);
		assertThat(coordenadas[3]).containsExactly(-46.630, -23.553);
	}

	@Test
	void classificaSituacaoSinalConformeMinutosSemTransmissao() {
		Escala escala = escala(10L);
		when(escalaRepository.findByEmpresaIdAndDataAndStatusAndAtivoTrue(EMPRESA_ID, HOJE, StatusEscala.PUBLICADA))
			.thenReturn(List.of(escala));
		when(trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(escala.getId()))
			.thenReturn(List.of());

		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(
			EMPRESA_ID, equipamentoDa(escala).getId()
		)).thenReturn(Optional.of(posicao(Instant.now().minus(5, ChronoUnit.MINUTES))));
		assertThat(service.obter(EMPRESA_ID, HOJE).ativos().get(0).situacaoSinal()).isEqualTo(SituacaoSinal.ONLINE);

		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(
			EMPRESA_ID, equipamentoDa(escala).getId()
		)).thenReturn(Optional.of(posicao(Instant.now().minus(45, ChronoUnit.MINUTES))));
		assertThat(service.obter(EMPRESA_ID, HOJE).ativos().get(0).situacaoSinal()).isEqualTo(SituacaoSinal.ATENCAO);

		when(posicaoGpsRepository.findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(
			EMPRESA_ID, equipamentoDa(escala).getId()
		)).thenReturn(Optional.of(posicao(Instant.now().minus(90, ChronoUnit.MINUTES))));
		assertThat(service.obter(EMPRESA_ID, HOJE).ativos().get(0).situacaoSinal()).isEqualTo(SituacaoSinal.OFFLINE);
	}

	private Escala escala(Long id) {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		empresa.setCodigo("EMP");
		empresa.setNome("Empresa Teste");
		empresa.setAtivo(true);

		Setor setor = new Setor();
		setor.setId(1L);
		setor.setNome("Setor 1");

		Equipamento equipamento = new Equipamento();
		equipamento.setId(1L);
		equipamento.setCodigo("LUT-0001");

		Colaborador colaborador = new Colaborador();
		colaborador.setId(1L);
		colaborador.setNome("João Silva");
		colaborador.setMatricula("M-001");
		colaborador.setEquipamento(equipamento);

		Usuario supervisor = new Usuario();
		supervisor.setId(1L);
		supervisor.setNome("Supervisor 1");

		Escala escala = new Escala();
		escala.setId(id);
		escala.setEmpresa(empresa);
		escala.setData(HOJE);
		escala.setTurno(Turno.MANHA);
		escala.setSetor(setor);
		escala.getColaboradores().add(colaborador);
		escala.setSupervisor(supervisor);
		escala.setStatus(StatusEscala.PUBLICADA);
		escala.setAtivo(true);
		return escala;
	}

	private Equipamento equipamentoDa(Escala escala) {
		return escala.equipamentosDosColaboradores().iterator().next();
	}

	private Trecho trecho(Long id, String codigo, String wkt) {
		Setor setor = new Setor();
		setor.setId(1L);
		setor.setNome("Setor Teste");

		Trecho trecho = new Trecho();
		trecho.setId(id);
		trecho.setSetor(setor);
		trecho.setNomeRua("Rua Teste");
		trecho.setCodigo(codigo);
		trecho.setGeom(GeometriaWkt.lineStringDeWkt(wkt));
		trecho.setAtivo(true);
		return trecho;
	}

	private TrechoExecucao trechoExecucao(
		Long id,
		Escala escala,
		Trecho trecho,
		int ordem,
		StatusTrecho status,
		float percentual
	) {
		TrechoExecucao execucao = new TrechoExecucao();
		execucao.setId(id);
		execucao.setEmpresa(escala.getEmpresa());
		execucao.setEscala(escala);
		execucao.setTrecho(trecho);
		execucao.setOrdem(ordem);
		execucao.setStatus(status);
		execucao.setPercentualExecutado(percentual);
		execucao.setAtivo(true);
		return execucao;
	}

	private PosicaoTrajetoConsulta consulta(Instant capturadoEm, double latitude, double longitude, boolean valido) {
		return new PosicaoTrajetoConsulta(1L, GeometriaWkt.pontoDe(latitude, longitude), capturadoEm, valido);
	}

	private PosicaoGps posicao(Instant recebidoEm) {
		PosicaoGps posicao = new PosicaoGps();
		posicao.setGeom(GeometriaWkt.pontoDe(-23.55, -46.63));
		posicao.setCapturadoEm(recebidoEm);
		posicao.setRecebidoEm(recebidoEm);
		return posicao;
	}
}
