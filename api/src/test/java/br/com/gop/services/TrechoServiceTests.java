package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Setor;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoDesenhoRequest;
import br.com.gop.models.TrechoImportacaoResponse;
import br.com.gop.repositories.TrechoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrechoServiceTests {

	@Mock
	private EmpresaService empresaService;

	@Mock
	private SetorService setorService;

	@Mock
	private TrechoRepository trechoRepository;

	@Mock
	private AuditoriaService auditoriaService;

	private TrechoService service;

	@BeforeEach
	void configurar() {
		service = new TrechoService(empresaService, setorService, trechoRepository, auditoriaService);
	}

	@Test
	void deveCriarUmTrechoPorLinhaDesenhadaComCodigoGerado() {
		Empresa empresa = empresaAtiva();
		Setor setor = setorAtivo();
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(setorService.buscarEntidade(1L, 21L)).thenReturn(setor);
		when(trechoRepository.findByEmpresaIdOrderByCodigoAsc(1L)).thenReturn(List.of());
		AtomicLong sequencia = new AtomicLong(1);
		when(trechoRepository.save(any(Trecho.class))).thenAnswer(invocation -> {
			Trecho trecho = invocation.getArgument(0);
			trecho.setId(sequencia.getAndIncrement());
			return trecho;
		});

		TrechoImportacaoResponse resultado = service.criarDesenho(1L, new TrechoDesenhoRequest(
			21L,
			"Rua das Flores",
			null,
			List.of(
				"LINESTRING(-34.90 -8.05, -34.89 -8.04)",
				"LINESTRING(-34.88 -8.03, -34.87 -8.02)"
			),
			null
		));

		assertThat(resultado.lidos()).isEqualTo(2);
		assertThat(resultado.criados()).isEqualTo(2);
		assertThat(resultado.ignorados()).isEqualTo(0);
		ArgumentCaptor<Trecho> captor = ArgumentCaptor.forClass(Trecho.class);
		verify(trechoRepository, times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).extracting(Trecho::getCodigo)
			.containsExactly("DFA001-0001", "DFA001-0002");
		assertThat(captor.getAllValues()).extracting(Trecho::getNomeRua)
			.containsOnly("Rua das Flores");
		assertThat(captor.getAllValues()).extracting(Trecho::getStatus)
			.containsOnly(StatusTrecho.PLANEJADO);
	}

	@Test
	void deveUsarCodigoInformadoComoPrefixoNasLinhas() {
		Empresa empresa = empresaAtiva();
		Setor setor = setorAtivo();
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(setorService.buscarEntidade(1L, 21L)).thenReturn(setor);
		when(trechoRepository.findByEmpresaIdOrderByCodigoAsc(1L)).thenReturn(List.of());
		when(trechoRepository.save(any(Trecho.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.criarDesenho(1L, new TrechoDesenhoRequest(
			21L,
			null,
			"TR-A",
			List.of(
				"LINESTRING(-34.90 -8.05, -34.89 -8.04)",
				"LINESTRING(-34.88 -8.03, -34.87 -8.02)"
			),
			StatusTrecho.PENDENTE
		));

		ArgumentCaptor<Trecho> captor = ArgumentCaptor.forClass(Trecho.class);
		verify(trechoRepository, times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).extracting(Trecho::getCodigo)
			.containsExactly("TR-A", "TR-A-2");
		assertThat(captor.getAllValues()).extracting(Trecho::getStatus)
			.containsOnly(StatusTrecho.PENDENTE);
	}

	@Test
	void deveRecusarDesenhoSemLinhas() {
		Empresa empresa = empresaAtiva();
		Setor setor = setorAtivo();
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(setorService.buscarEntidade(1L, 21L)).thenReturn(setor);

		assertThatThrownBy(() -> service.criarDesenho(
			1L,
			new TrechoDesenhoRequest(21L, null, null, List.of(), null)
		)).isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("ao menos um trecho");
	}

	private Empresa empresaAtiva() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);
		empresa.setAtivo(true);
		return empresa;
	}

	private Setor setorAtivo() {
		Setor setor = new Setor();
		setor.setId(21L);
		setor.setCodigo("DFA001");
		setor.setNome("Setor Norte");
		setor.setAtivo(true);
		return setor;
	}
}
