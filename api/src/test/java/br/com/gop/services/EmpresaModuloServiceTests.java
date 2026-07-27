package br.com.gop.services;

import br.com.gop.models.CategoriaModulo;
import br.com.gop.models.Empresa;
import br.com.gop.models.EmpresaModulo;
import br.com.gop.models.EmpresaModuloRequest;
import br.com.gop.models.EmpresaModuloResponse;
import br.com.gop.models.Modulo;
import br.com.gop.repositories.EmpresaModuloRepository;
import br.com.gop.repositories.ModuloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaModuloServiceTests {

	@Mock
	private EmpresaService empresaService;

	@Mock
	private ModuloRepository moduloRepository;

	@Mock
	private EmpresaModuloRepository empresaModuloRepository;

	@Mock
	private AuditoriaService auditoriaService;

	private EmpresaModuloService service;

	@BeforeEach
	void configurar() {
		service = new EmpresaModuloService(
			empresaService,
			moduloRepository,
			empresaModuloRepository,
			auditoriaService
		);
	}

	@Test
	void deveHabilitarModuloCriandoVinculo() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);
		empresa.setAtivo(true);

		Modulo modulo = new Modulo();
		modulo.setId(9L);
		modulo.setCodigo("ESCALA");
		modulo.setNome("Escala");
		modulo.setCategoria(CategoriaModulo.OPERACIONAL);
		modulo.setOrdem(10);
		modulo.setAtivo(true);

		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(moduloRepository.findById(9L)).thenReturn(Optional.of(modulo));
		when(empresaModuloRepository.findByEmpresaIdAndModuloId(1L, 9L)).thenReturn(Optional.empty());
		when(empresaModuloRepository.save(any(EmpresaModulo.class))).thenAnswer(invocation -> {
			EmpresaModulo vinculo = invocation.getArgument(0);
			vinculo.setId(33L);
			return vinculo;
		});

		EmpresaModuloResponse response = service.atualizar(
			1L,
			9L,
			new EmpresaModuloRequest(true)
		);

		ArgumentCaptor<EmpresaModulo> captor = ArgumentCaptor.forClass(EmpresaModulo.class);
		verify(empresaModuloRepository).save(captor.capture());
		assertThat(captor.getValue().isHabilitado()).isTrue();
		assertThat(captor.getValue().getDesde()).isNotNull();
		assertThat(response.habilitado()).isTrue();
		assertThat(response.codigo()).isEqualTo("ESCALA");
	}

	@Test
	void deveListarCatalogoMesclandoStatus() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);

		Modulo modulo = new Modulo();
		modulo.setId(9L);
		modulo.setCodigo("ESCALA");
		modulo.setNome("Escala");
		modulo.setCategoria(CategoriaModulo.OPERACIONAL);
		modulo.setOrdem(10);
		modulo.setAtivo(true);

		EmpresaModulo vinculo = new EmpresaModulo();
		vinculo.setModulo(modulo);
		vinculo.setHabilitado(true);

		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(moduloRepository.findByAtivoTrueOrderByOrdemAscNomeAsc()).thenReturn(List.of(modulo));
		when(empresaModuloRepository.findByEmpresaIdWithModulo(1L)).thenReturn(List.of(vinculo));

		List<EmpresaModuloResponse> lista = service.listarPorEmpresa(1L);

		assertThat(lista).hasSize(1);
		assertThat(lista.get(0).habilitado()).isTrue();
	}
}
