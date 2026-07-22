package br.com.neutrowaste.gop.services;

import br.com.neutrowaste.gop.models.Empresa;
import br.com.neutrowaste.gop.models.EmpresaRequest;
import br.com.neutrowaste.gop.repositories.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTests {

	@Mock
	private EmpresaRepository empresaRepository;

	@Mock
	private AuditoriaService auditoriaService;

	private EmpresaService service;

	@BeforeEach
	void configurar() {
		service = new EmpresaService(empresaRepository, auditoriaService);
	}

	@Test
	void deveCriarEmpresaNormalizandoCodigo() {
		doAnswer(invocation -> {
			Empresa empresa = invocation.getArgument(0);
			empresa.setId(5L);
			return empresa;
		}).when(empresaRepository).save(any(Empresa.class));

		var response = service.criar(new EmpresaRequest(" cliente-sp ", " Cliente SP ", null));

		ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
		verify(empresaRepository).save(captor.capture());
		assertThat(captor.getValue().getCodigo()).isEqualTo("CLIENTE-SP");
		assertThat(captor.getValue().getNome()).isEqualTo("Cliente SP");
		assertThat(captor.getValue().isAtivo()).isTrue();
		assertThat(response.id()).isEqualTo(5L);
		verify(auditoriaService).registrar(
			captor.getValue(),
			"EMPRESA_CRIADA",
			"Empresa",
			5L,
			java.util.Map.of(
				"codigo", "CLIENTE-SP",
				"nome", "Cliente SP",
				"ativo", true
			)
		);
	}

	@Test
	void deveRejeitarCodigoDuplicado() {
		when(empresaRepository.existsByCodigoIgnoreCase("CLIENTE")).thenReturn(true);

		assertThatThrownBy(
			() -> service.criar(new EmpresaRequest("cliente", "Cliente", true))
		)
			.isInstanceOf(ConflitoNegocioException.class);

		verify(empresaRepository, never()).save(any());
	}
}
