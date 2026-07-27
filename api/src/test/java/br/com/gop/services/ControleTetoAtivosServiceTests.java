package br.com.gop.services;

import br.com.gop.models.Licenca;
import br.com.gop.models.StatusAtivo;
import br.com.gop.models.StatusLicenca;
import br.com.gop.repositories.AtivoRepository;
import br.com.gop.repositories.LicencaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControleTetoAtivosServiceTests {

	@Mock
	private LicencaRepository licencaRepository;

	@Mock
	private AtivoRepository ativoRepository;

	private ControleTetoAtivosService service;

	@BeforeEach
	void configurar() {
		service = new ControleTetoAtivosService(licencaRepository, ativoRepository);
	}

	@Test
	void deveCalcularConsumoESaldoDoTeto() {
		Licenca licenca = new Licenca();
		licenca.setId(7L);
		licenca.setTetoAtivos(10);
		when(licencaRepository.findVigente(3L, StatusLicenca.ATIVA, LocalDate.now()))
			.thenReturn(Optional.of(licenca));
		when(ativoRepository.countByEmpresaIdAndAtivoTrueAndStatus(3L, StatusAtivo.ATIVO))
			.thenReturn(8L);

		var teto = service.consultar(3L);

		assertThat(teto.tetoAtivos()).isEqualTo(10);
		assertThat(teto.ativosEmUso()).isEqualTo(8);
		assertThat(teto.saldoDisponivel()).isEqualTo(2);
		assertThat(teto.permiteNovoAtivo()).isTrue();
	}

	@Test
	void deveBloquearReducaoAbaixoDosAtivosEmUso() {
		when(ativoRepository.countByEmpresaIdAndAtivoTrueAndStatus(3L, StatusAtivo.ATIVO))
			.thenReturn(8L);

		assertThatThrownBy(() -> service.validarNovoTeto(3L, 7))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("8");

		verify(ativoRepository)
			.countByEmpresaIdAndAtivoTrueAndStatus(3L, StatusAtivo.ATIVO);
	}

	@Test
	void deveBloquearNovoAtivoQuandoTetoFoiAtingido() {
		Licenca licenca = new Licenca();
		licenca.setId(7L);
		licenca.setTetoAtivos(2);
		when(licencaRepository.findVigente(3L, StatusLicenca.ATIVA, LocalDate.now()))
			.thenReturn(Optional.of(licenca));
		when(ativoRepository.countByEmpresaIdAndAtivoTrueAndStatus(3L, StatusAtivo.ATIVO))
			.thenReturn(2L);

		assertThatThrownBy(() -> service.validarNovoAtivo(3L))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("atingido");
	}
}
