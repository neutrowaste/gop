package br.com.gop.services;

import br.com.gop.models.Colaborador;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.EscalaRequest;
import br.com.gop.models.EscalaResponse;
import br.com.gop.models.Setor;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.TipoEscala;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.Turno;
import br.com.gop.models.Usuario;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.TrechoExecucaoRepository;
import br.com.gop.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalaServiceTests {

	private static final ZoneId FUSO = EscalaService.FUSO_OPERACIONAL;
	private static final LocalDate QUARTA = LocalDate.of(2026, 8, 19);
	private static final Instant INSTANTE = QUARTA.atTime(10, 0).atZone(FUSO).toInstant();

	@Mock
	private EmpresaService empresaService;
	@Mock
	private SetorService setorService;
	@Mock
	private ColaboradorService colaboradorService;
	@Mock
	private TrechoService trechoService;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private EscalaRepository escalaRepository;
	@Mock
	private TrechoExecucaoRepository trechoExecucaoRepository;
	@Mock
	private AuditoriaService auditoriaService;

	private EscalaService service;
	private Empresa empresa;
	private Setor setor;
	private Usuario supervisor;
	private Colaborador colaborador;
	private Colaborador outroColaborador;

	@BeforeEach
	void configurar() {
		service = new EscalaService(
			empresaService,
			setorService,
			colaboradorService,
			trechoService,
			usuarioRepository,
			escalaRepository,
			trechoExecucaoRepository,
			auditoriaService,
			Clock.fixed(INSTANTE, FUSO)
		);
		empresa = empresa(1L);
		setor = setor(4L);
		supervisor = supervisor(8L);
		colaborador = colaborador(11L, "Ana", 21L, "LUT-0001");
		outroColaborador = colaborador(12L, "Beto", 22L, "LUT-0002");

		lenient().when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		lenient().when(setorService.buscarEntidade(1L, 4L)).thenReturn(setor);
		lenient().when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(supervisor));
		lenient().when(colaboradorService.buscarEntidades(eq(1L), any())).thenAnswer(invocation -> {
			List<Long> ids = invocation.getArgument(1);
			return ids.stream()
				.map(id -> id.equals(12L) ? outroColaborador : colaborador)
				.toList();
		});
		lenient().when(escalaRepository.findAtivasComColaboradoresNoTurno(anyLong(), any(), any(), anyCollection()))
			.thenReturn(List.of());
		lenient().when(escalaRepository.findAtivasComColaboradoresNoTurnoExcluindo(
			anyLong(), any(), any(), anyCollection(), anyLong()
		)).thenReturn(List.of());
		lenient().when(escalaRepository.findAtivasComEquipamentosNoTurno(anyLong(), any(), any(), anyCollection()))
			.thenReturn(List.of());
		lenient().when(escalaRepository.findAtivasComEquipamentosNoTurnoExcluindo(
			anyLong(), any(), any(), anyCollection(), anyLong()
		)).thenReturn(List.of());
		lenient().when(escalaRepository.findByEmpresaIdAndTipoAndOrigemIsNullAndAtivoTrue(1L, TipoEscala.FREQUENCIA))
			.thenReturn(List.of());
		lenient().when(escalaRepository.findByOrigemIdAndAtivoTrue(anyLong())).thenReturn(List.of());
		lenient().when(trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(anyLong()))
			.thenReturn(List.of());
		AtomicLong sequencia = new AtomicLong(1);
		lenient().when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> {
			Escala escala = invocation.getArgument(0);
			if (escala.getId() == null) {
				escala.setId(sequencia.getAndIncrement());
			}
			return escala;
		});
	}

	@Test
	void avulsaNascePublicada() {
		EscalaResponse response = service.criar(1L, pedidoAvulsa(QUARTA, List.of(11L)));

		ArgumentCaptor<Escala> captor = ArgumentCaptor.forClass(Escala.class);
		verify(escalaRepository).save(captor.capture());
		Escala salva = captor.getValue();
		assertThat(salva.getTipo()).isEqualTo(TipoEscala.AVULSA);
		assertThat(salva.getStatus()).isEqualTo(StatusEscala.PUBLICADA);
		assertThat(salva.getData()).isEqualTo(QUARTA);
		assertThat(salva.getPublicadaEm()).isNotNull();
		assertThat(response.tipo()).isEqualTo(TipoEscala.AVULSA);
		assertThat(response.status()).isEqualTo(StatusEscala.PUBLICADA);
	}

	@Test
	void frequenciaGeraInstanciaQuandoHojeEstaNosDiasMarcados() {
		when(escalaRepository.existsByOrigemIdAndData(1L, QUARTA)).thenReturn(false);

		EscalaResponse response = service.criar(
			1L,
			pedidoFrequencia(List.of(DayOfWeek.WEDNESDAY), List.of(11L))
		);

		assertThat(response.tipo()).isEqualTo(TipoEscala.FREQUENCIA);
		assertThat(response.data()).isNull();
		assertThat(response.diasSemana()).containsExactly(DayOfWeek.WEDNESDAY);
		verify(escalaRepository, times(2)).save(any(Escala.class));
		verify(auditoriaService).registrar(eq(empresa), eq("ESCALA_FREQUENCIA_GERADA"), eq("Escala"), eq(2L), any());
	}

	@Test
	void frequenciaNaoGeraInstanciaQuandoHojeNaoEstaNosDiasMarcados() {
		EscalaResponse response = service.criar(
			1L,
			pedidoFrequencia(List.of(DayOfWeek.MONDAY), List.of(11L))
		);

		assertThat(response.data()).isNull();
		verify(escalaRepository, times(1)).save(any(Escala.class));
		verify(escalaRepository, never()).existsByOrigemIdAndData(anyLong(), any());
	}

	@Test
	void trocaDeColaboradoresNoModeloReplicaSoParaInstanciaDeHoje() {
		Escala modelo = modeloFrequencia(1L, Set.of(DayOfWeek.WEDNESDAY), colaborador);
		Escala instanciaHoje = instancia(2L, modelo, QUARTA, colaborador);
		when(escalaRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(modelo));
		when(escalaRepository.findByOrigemIdAndDataAndAtivoTrue(1L, QUARTA)).thenReturn(Optional.of(instanciaHoje));

		service.atualizar(1L, 1L, pedidoFrequencia(List.of(DayOfWeek.WEDNESDAY), List.of(12L)));

		assertThat(modelo.getColaboradores()).extracting(Colaborador::getId).containsExactly(12L);
		assertThat(instanciaHoje.getColaboradores()).extracting(Colaborador::getId).containsExactly(12L);
	}

	@Test
	void cancelarModeloCancelaTodasAsInstanciasAtivas() {
		Escala modelo = modeloFrequencia(1L, Set.of(DayOfWeek.WEDNESDAY), colaborador);
		Escala instanciaHoje = instancia(2L, modelo, QUARTA, colaborador);
		when(escalaRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(modelo));
		when(escalaRepository.findByOrigemIdAndAtivoTrue(1L)).thenReturn(List.of(instanciaHoje));

		service.desativar(1L, 1L);

		assertThat(modelo.isAtivo()).isFalse();
		assertThat(instanciaHoje.isAtivo()).isFalse();
	}

	@Test
	void cancelarInstanciaCancelaOModeloELiberaColaboradores() {
		Escala modelo = modeloFrequencia(1L, Set.of(DayOfWeek.WEDNESDAY), colaborador);
		Escala instanciaHoje = instancia(2L, modelo, QUARTA, colaborador);
		when(escalaRepository.findByIdAndEmpresaId(2L, 1L)).thenReturn(Optional.of(instanciaHoje));
		when(escalaRepository.findByOrigemIdAndAtivoTrue(1L)).thenReturn(List.of(instanciaHoje));

		service.desativar(1L, 2L);

		assertThat(instanciaHoje.isAtivo()).isFalse();
		assertThat(modelo.isAtivo()).isFalse();
	}

	@Test
	void cancelarModeloImpedeNovaGeracao() {
		Escala modelo = modeloFrequencia(1L, Set.of(DayOfWeek.WEDNESDAY), colaborador);
		modelo.setAtivo(false);
		when(escalaRepository.findByTipoAndOrigemIsNullAndAtivoTrue(TipoEscala.FREQUENCIA))
			.thenReturn(List.of(modelo));

		int geradas = service.gerarInstanciasDoDia(QUARTA);

		assertThat(geradas).isZero();
		verify(escalaRepository, never()).save(any(Escala.class));
	}

	@Test
	void rejeitaDuasFrequenciasComMesmoColaboradorTurnoEDiaSobreposto() {
		Escala existente = modeloFrequencia(9L, Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), colaborador);
		when(escalaRepository.findByEmpresaIdAndTipoAndOrigemIsNullAndAtivoTrue(1L, TipoEscala.FREQUENCIA))
			.thenReturn(List.of(existente));

		assertThatThrownBy(() -> service.criar(
			1L,
			pedidoFrequencia(List.of(DayOfWeek.WEDNESDAY), List.of(11L))
		))
			.isInstanceOf(ConflitoNegocioException.class)
			.hasMessageContaining("já estão em outra escala de frequência");
		verify(escalaRepository, never()).save(any(Escala.class));
	}

	@Test
	void copiaPlanoDeTrechosAoGerarODia() {
		Escala modelo = modeloFrequencia(1L, Set.of(DayOfWeek.WEDNESDAY), colaborador);
		Trecho trecho = new Trecho();
		trecho.setId(50L);
		trecho.setCodigo("T-01");
		trecho.setSetor(setor);
		TrechoExecucao original = new TrechoExecucao();
		original.setId(70L);
		original.setEscala(modelo);
		original.setTrecho(trecho);
		original.setOrdem(1);
		original.setStatus(StatusTrecho.PLANEJADO);
		original.setAtivo(true);

		when(escalaRepository.findByTipoAndOrigemIsNullAndAtivoTrue(TipoEscala.FREQUENCIA))
			.thenReturn(List.of(modelo));
		when(escalaRepository.existsByOrigemIdAndData(1L, QUARTA)).thenReturn(false);
		when(trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(1L))
			.thenReturn(List.of(original));

		int geradas = service.gerarInstanciasDoDia(QUARTA);

		assertThat(geradas).isEqualTo(1);
		ArgumentCaptor<TrechoExecucao> captor = ArgumentCaptor.forClass(TrechoExecucao.class);
		verify(trechoExecucaoRepository).save(captor.capture());
		assertThat(captor.getValue().getTrecho().getId()).isEqualTo(50L);
		assertThat(captor.getValue().getOrdem()).isEqualTo(1);
		assertThat(captor.getValue().getStatus()).isEqualTo(StatusTrecho.PLANEJADO);
		assertThat(captor.getValue().getEscala().getOrigem()).isEqualTo(modelo);
	}

	@Test
	void criaAvulsaComTrechosDoSetorNoPlano() {
		Trecho trecho = new Trecho();
		trecho.setId(50L);
		trecho.setCodigo("T-01");
		trecho.setSetor(setor);
		trecho.setAtivo(true);
		when(trechoService.buscarEntidade(1L, 50L)).thenReturn(trecho);

		service.criar(1L, new EscalaRequest(
			TipoEscala.AVULSA,
			QUARTA,
			List.of(),
			Turno.MANHA,
			4L,
			List.of(11L),
			8L,
			List.of(50L),
			true
		));

		ArgumentCaptor<TrechoExecucao> captor = ArgumentCaptor.forClass(TrechoExecucao.class);
		verify(trechoExecucaoRepository).save(captor.capture());
		assertThat(captor.getValue().getTrecho().getId()).isEqualTo(50L);
		assertThat(captor.getValue().getOrdem()).isEqualTo(1);
		assertThat(captor.getValue().getStatus()).isEqualTo(StatusTrecho.PLANEJADO);
	}

	private EscalaRequest pedidoAvulsa(LocalDate data, List<Long> colaboradorIds) {
		return new EscalaRequest(
			TipoEscala.AVULSA,
			data,
			List.of(),
			Turno.MANHA,
			4L,
			colaboradorIds,
			8L,
			List.of(),
			true
		);
	}

	private EscalaRequest pedidoFrequencia(List<DayOfWeek> dias, List<Long> colaboradorIds) {
		return new EscalaRequest(
			TipoEscala.FREQUENCIA,
			null,
			dias,
			Turno.MANHA,
			4L,
			colaboradorIds,
			8L,
			List.of(),
			true
		);
	}

	private Escala modeloFrequencia(Long id, Set<DayOfWeek> dias, Colaborador daEquipe) {
		Escala modelo = new Escala();
		modelo.setId(id);
		modelo.setEmpresa(empresa);
		modelo.setTipo(TipoEscala.FREQUENCIA);
		modelo.setTurno(Turno.MANHA);
		modelo.setSetor(setor);
		modelo.getColaboradores().add(daEquipe);
		modelo.setSupervisor(supervisor);
		modelo.getDiasSemana().addAll(dias);
		modelo.setStatus(StatusEscala.PUBLICADA);
		modelo.setAtivo(true);
		return modelo;
	}

	private Escala instancia(Long id, Escala modelo, LocalDate data, Colaborador daEquipe) {
		Escala gerada = new Escala();
		gerada.setId(id);
		gerada.setEmpresa(empresa);
		gerada.setTipo(TipoEscala.FREQUENCIA);
		gerada.setOrigem(modelo);
		gerada.setData(data);
		gerada.setTurno(modelo.getTurno());
		gerada.setSetor(setor);
		gerada.getColaboradores().add(daEquipe);
		gerada.setSupervisor(supervisor);
		gerada.setStatus(StatusEscala.PUBLICADA);
		gerada.setAtivo(true);
		return gerada;
	}

	private static Empresa empresa(Long id) {
		Empresa item = new Empresa();
		item.setId(id);
		item.setCodigo("EMP");
		item.setNome("Empresa Teste");
		item.setAtivo(true);
		return item;
	}

	private static Setor setor(Long id) {
		Setor item = new Setor();
		item.setId(id);
		item.setNome("Setor Norte");
		return item;
	}

	private static Usuario supervisor(Long id) {
		Usuario item = new Usuario();
		item.setId(id);
		item.setNome("Supervisora");
		item.setAtivo(true);
		return item;
	}

	private static Colaborador colaborador(Long id, String nome, Long equipamentoId, String codigo) {
		Equipamento equipamento = new Equipamento();
		equipamento.setId(equipamentoId);
		equipamento.setCodigo(codigo);
		equipamento.setAtivo(true);
		Colaborador item = new Colaborador();
		item.setId(id);
		item.setNome(nome);
		item.setMatricula("M-" + id);
		item.setEquipamento(equipamento);
		item.setAtivo(true);
		return item;
	}
}
