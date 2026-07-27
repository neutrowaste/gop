package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.LicencaRequest;
import br.com.gop.models.StatusLicenca;
import br.com.gop.repositories.LicencaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LicencaServiceTests {

	@Mock
	private LicencaRepository licencaRepository;

	@Mock
	private EmpresaService empresaService;

	@Mock
	private ControleTetoAtivosService controleTetoAtivosService;

	@Mock
	private AuditoriaService auditoriaService;

	private LicencaService service;

	@BeforeEach
	void configurar() {
		service = new LicencaService(
			licencaRepository,
			empresaService,
			controleTetoAtivosService,
			auditoriaService
		);
	}

	@Test
	void deveRejeitarPeriodoInvertido() {
		Empresa empresa = empresaAtiva();
		when(empresaService.buscarEntidade(4L)).thenReturn(empresa);
		LicencaRequest request = new LicencaRequest(
			10,
			LocalDate.of(2026, 8, 2),
			LocalDate.of(2026, 8, 1),
			StatusLicenca.ATIVA,
			true
		);

		assertThatThrownBy(() -> service.criar(4L, request))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("data final");

		verify(licencaRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void deveRejeitarLicencaSobreposta() {
		Empresa empresa = empresaAtiva();
		when(empresaService.buscarEntidade(4L)).thenReturn(empresa);
		LicencaRequest request = new LicencaRequest(
			10,
			LocalDate.of(2026, 8, 1),
			LocalDate.of(2026, 8, 31),
			StatusLicenca.ATIVA,
			true
		);
		when(licencaRepository.existeSobreposicao(
			4L,
			request.vigenciaInicio(),
			request.vigenciaFim(),
			null,
			StatusLicenca.ENCERRADA
		)).thenReturn(true);

		assertThatThrownBy(() -> service.criar(4L, request))
			.isInstanceOf(ConflitoNegocioException.class)
			.hasMessageContaining("período");

		verify(controleTetoAtivosService, never())
			.validarNovoTeto(4L, request.tetoAtivos());
	}

	private Empresa empresaAtiva() {
		Empresa empresa = new Empresa();
		empresa.setId(4L);
		empresa.setCodigo("CLIENTE");
		empresa.setNome("Cliente");
		empresa.setAtivo(true);
		return empresa;
	}
}
