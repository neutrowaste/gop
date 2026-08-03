package br.com.gop.services;

import br.com.gop.models.ParametroEmpresa;
import br.com.gop.repositories.ParametroEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** RN-001: limiares do motor caem para o default documentado quando a empresa não configurou a chave. */
@ExtendWith(MockitoExtension.class)
class ParametroEmpresaServiceTests {

	@Mock
	private EmpresaService empresaService;

	@Mock
	private ParametroEmpresaRepository parametroEmpresaRepository;

	@Mock
	private AuditoriaService auditoriaService;

	private ParametroEmpresaService service;

	@BeforeEach
	void configurar() {
		service = new ParametroEmpresaService(empresaService, parametroEmpresaRepository, auditoriaService);
	}

	@Test
	void usaDefaultQuandoEmpresaNaoConfigurouCoberturaMinima() {
		when(parametroEmpresaRepository.findByEmpresaIdAndChaveAndAtivoTrue(1L, ParametroEmpresaService.COBERTURA_MINIMA_PCT))
			.thenReturn(Optional.empty());

		assertThat(service.coberturaMinimaPercentual(1L)).isEqualTo(80.0);
	}

	@Test
	void usaValorConfiguradoQuandoEmpresaDefiniuCoberturaMinima() {
		ParametroEmpresa parametro = new ParametroEmpresa();
		parametro.setChave(ParametroEmpresaService.COBERTURA_MINIMA_PCT);
		parametro.setValor("90");
		when(parametroEmpresaRepository.findByEmpresaIdAndChaveAndAtivoTrue(1L, ParametroEmpresaService.COBERTURA_MINIMA_PCT))
			.thenReturn(Optional.of(parametro));

		assertThat(service.coberturaMinimaPercentual(1L)).isEqualTo(90.0);
	}

	@Test
	void usaDefaultDeSilencioGpsDeSessentaMinutos() {
		when(parametroEmpresaRepository.findByEmpresaIdAndChaveAndAtivoTrue(1L, ParametroEmpresaService.SILENCIO_GPS_MINUTOS))
			.thenReturn(Optional.empty());

		assertThat(service.silencioGpsMinutos(1L)).isEqualTo(60L);
	}
}
