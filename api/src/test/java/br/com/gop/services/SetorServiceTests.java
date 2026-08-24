package br.com.gop.services;

import br.com.gop.models.Cidade;
import br.com.gop.models.Empresa;
import br.com.gop.models.Modulo;
import br.com.gop.models.Setor;
import br.com.gop.models.SetorRequest;
import br.com.gop.models.SetorResponse;
import br.com.gop.repositories.EmpresaModuloRepository;
import br.com.gop.repositories.ModuloRepository;
import br.com.gop.repositories.SetorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetorServiceTests {

	@Mock
	private EmpresaService empresaService;

	@Mock
	private CidadeService cidadeService;

	@Mock
	private ModuloRepository moduloRepository;

	@Mock
	private EmpresaModuloRepository empresaModuloRepository;

	@Mock
	private SetorRepository setorRepository;

	@Mock
	private AuditoriaService auditoriaService;

	private SetorService service;

	@BeforeEach
	void configurar() {
		service = new SetorService(
			empresaService,
			cidadeService,
			moduloRepository,
			empresaModuloRepository,
			setorRepository,
			auditoriaService
		);
	}

	@Test
	void deveCriarSetorVinculadoAoServicoContratado() {
		Empresa empresa = empresaAtiva();
		Cidade cidade = cidade();
		Modulo modulo = modulo(9L, "VARRIACAO_VIAS", "Varrição de Vias");

		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(moduloRepository.findById(9L)).thenReturn(Optional.of(modulo));
		when(empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(1L)).thenReturn(Set.of(9L));
		when(cidadeService.buscarEntidade(1L, 4L)).thenReturn(cidade);
		when(setorRepository.existsByEmpresaIdAndModuloIdAndCodigoIgnoreCase(1L, 9L, "DFA001"))
			.thenReturn(false);
		when(setorRepository.save(any(Setor.class))).thenAnswer(invocation -> {
			Setor setor = invocation.getArgument(0);
			setor.setId(21L);
			return setor;
		});

		SetorResponse response = service.criar(1L, new SetorRequest(9L, 4L, "dfa001", "Setor Norte", true));

		ArgumentCaptor<Setor> captor = ArgumentCaptor.forClass(Setor.class);
		verify(setorRepository).save(captor.capture());
		assertThat(captor.getValue().getModulo().getId()).isEqualTo(9L);
		assertThat(captor.getValue().getCodigo()).isEqualTo("DFA001");
		assertThat(response.moduloId()).isEqualTo(9L);
		assertThat(response.moduloCodigo()).isEqualTo("VARRIACAO_VIAS");
		assertThat(response.codigo()).isEqualTo("DFA001");
	}

	@Test
	void deveRejeitarSetorSemServicoContratado() {
		when(empresaService.buscarEntidade(1L)).thenReturn(empresaAtiva());
		when(moduloRepository.findById(9L)).thenReturn(Optional.of(modulo(9L, "VARRIACAO_VIAS", "Varrição de Vias")));
		when(empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(1L)).thenReturn(Set.of());

		assertThatThrownBy(() -> service.criar(1L, new SetorRequest(9L, 4L, "DFA001", "Setor Norte", true)))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessage("O setor deve estar vinculado a um serviço contratado pela empresa");
		verify(setorRepository, never()).save(any());
	}

	@Test
	void deveRejeitarCodigoDuplicadoNoMesmoServico() {
		Modulo modulo = modulo(9L, "VARRIACAO_VIAS", "Varrição de Vias");
		when(empresaService.buscarEntidade(1L)).thenReturn(empresaAtiva());
		when(moduloRepository.findById(9L)).thenReturn(Optional.of(modulo));
		when(empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(1L)).thenReturn(Set.of(9L));
		when(cidadeService.buscarEntidade(1L, 4L)).thenReturn(cidade());
		when(setorRepository.existsByEmpresaIdAndModuloIdAndCodigoIgnoreCase(1L, 9L, "DFA001"))
			.thenReturn(true);

		assertThatThrownBy(() -> service.criar(1L, new SetorRequest(9L, 4L, "DFA001", "Setor Norte", true)))
			.isInstanceOf(ConflitoNegocioException.class)
			.hasMessage("Já existe setor com este código neste serviço");
		verify(setorRepository, never()).save(any());
	}

	@Test
	void devePermitirMesmoCodigoEmServicosDiferentes() {
		Empresa empresa = empresaAtiva();
		Cidade cidade = cidade();
		Modulo coleta = modulo(11L, "COLETA_PAPELEIRAS", "Coleta de Papeleiras");

		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(moduloRepository.findById(11L)).thenReturn(Optional.of(coleta));
		when(empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(1L)).thenReturn(Set.of(9L, 11L));
		when(cidadeService.buscarEntidade(1L, 4L)).thenReturn(cidade);
		when(setorRepository.existsByEmpresaIdAndModuloIdAndCodigoIgnoreCase(1L, 11L, "DFA001"))
			.thenReturn(false);
		when(setorRepository.save(any(Setor.class))).thenAnswer(invocation -> {
			Setor setor = invocation.getArgument(0);
			setor.setId(22L);
			return setor;
		});

		SetorResponse response = service.criar(1L, new SetorRequest(11L, 4L, "DFA001", "Setor Norte Coleta", true));

		verify(setorRepository).existsByEmpresaIdAndModuloIdAndCodigoIgnoreCase(1L, 11L, "DFA001");
		assertThat(response.moduloId()).isEqualTo(11L);
		assertThat(response.codigo()).isEqualTo("DFA001");
		assertThat(response.temDemarcacao()).isFalse();
	}

	@Test
	void deveRejeitarShapefileQueNaoEZip() {
		when(empresaService.buscarEntidade(1L)).thenReturn(empresaAtiva());
		Setor setor = setorComRelacionamentos();
		when(setorRepository.findByIdAndEmpresaId(21L, 1L)).thenReturn(Optional.of(setor));

		org.springframework.mock.web.MockMultipartFile arquivo = new org.springframework.mock.web.MockMultipartFile(
			"arquivo",
			"area.shp",
			"application/octet-stream",
			new byte[] {1, 2, 3}
		);

		assertThatThrownBy(() -> service.importarShapefile(1L, 21L, arquivo, null))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining(".zip");
	}

	@Test
	void deveRemoverDemarcacao() {
		when(empresaService.buscarEntidade(1L)).thenReturn(empresaAtiva());
		Setor setor = setorComRelacionamentos();
		when(setorRepository.findByIdAndEmpresaId(21L, 1L)).thenReturn(Optional.of(setor));
		when(setorRepository.save(any(Setor.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SetorResponse response = service.removerDemarcacao(1L, 21L);

		assertThat(setor.getGeom()).isNull();
		assertThat(response.temDemarcacao()).isFalse();
	}

	@Test
	void deveAtualizarDemarcacaoComWkt() {
		when(empresaService.buscarEntidade(1L)).thenReturn(empresaAtiva());
		Setor setor = setorComRelacionamentos();
		when(setorRepository.findByIdAndEmpresaId(21L, 1L)).thenReturn(Optional.of(setor));
		when(setorRepository.save(any(Setor.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SetorResponse response = service.atualizarDemarcacao(
			1L,
			21L,
			"POLYGON((-34.90 -8.05, -34.89 -8.05, -34.89 -8.04, -34.90 -8.04, -34.90 -8.05))"
		);

		assertThat(setor.getGeom()).isNotNull();
		assertThat(response.temDemarcacao()).isTrue();
		assertThat(response.geomWkt()).contains("POLYGON");
		assertThat(response.geomGeoJson()).isNotNull();
		assertThat(response.geomGeoJson().type()).isEqualTo("MultiPolygon");
	}

	@Test
	void deveRejeitarWktQueNaoEPoligono() {
		when(empresaService.buscarEntidade(1L)).thenReturn(empresaAtiva());
		Setor setor = setorComRelacionamentos();
		when(setorRepository.findByIdAndEmpresaId(21L, 1L)).thenReturn(Optional.of(setor));

		assertThatThrownBy(() -> service.atualizarDemarcacao(1L, 21L, "LINESTRING(-34.9 -8.05, -34.88 -8.04)"))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("Polygon");
	}

	private Setor setorComRelacionamentos() {
		Setor setor = new Setor();
		setor.setId(21L);
		setor.setAtivo(true);
		setor.setNome("Setor Norte");
		setor.setModulo(modulo(9L, "VARRIACAO_VIAS", "Varrição de Vias"));
		setor.setCidade(cidade());
		return setor;
	}

	private Empresa empresaAtiva() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);
		empresa.setAtivo(true);
		return empresa;
	}

	private Cidade cidade() {
		Cidade cidade = new Cidade();
		cidade.setId(4L);
		cidade.setNome("Recife");
		cidade.setUf("PE");
		return cidade;
	}

	private Modulo modulo(Long id, String codigo, String nome) {
		Modulo modulo = new Modulo();
		modulo.setId(id);
		modulo.setCodigo(codigo);
		modulo.setNome(nome);
		modulo.setAtivo(true);
		return modulo;
	}
}
