package br.com.gop.services;

import br.com.gop.models.CategoriaModulo;
import br.com.gop.models.Empresa;
import br.com.gop.models.Modulo;
import br.com.gop.models.Usuario;
import br.com.gop.models.UsuarioModulo;
import br.com.gop.models.UsuarioModuloRequest;
import br.com.gop.repositories.EmpresaModuloRepository;
import br.com.gop.repositories.ModuloRepository;
import br.com.gop.repositories.UsuarioModuloRepository;
import br.com.gop.repositories.UsuarioRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioModuloServiceTests {

	@Mock
	private EmpresaService empresaService;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private ModuloRepository moduloRepository;

	@Mock
	private EmpresaModuloRepository empresaModuloRepository;

	@Mock
	private UsuarioModuloRepository usuarioModuloRepository;

	@Mock
	private AuditoriaService auditoriaService;

	private UsuarioModuloService service;

	@BeforeEach
	void configurar() {
		service = new UsuarioModuloService(
			empresaService,
			usuarioRepository,
			moduloRepository,
			empresaModuloRepository,
			usuarioModuloRepository,
			auditoriaService
		);
	}

	@Test
	void deveLiberarModuloContratado() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);
		empresa.setAtivo(true);

		Usuario usuario = new Usuario();
		usuario.setId(8L);
		usuario.setEmpresa(empresa);

		Modulo modulo = new Modulo();
		modulo.setId(9L);
		modulo.setCodigo("ESCALA");
		modulo.setNome("Escala");
		modulo.setCategoria(CategoriaModulo.OPERACIONAL);
		modulo.setOrdem(10);
		modulo.setAtivo(true);

		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(usuario));
		when(moduloRepository.findById(9L)).thenReturn(Optional.of(modulo));
		when(empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(1L)).thenReturn(Set.of(9L));
		when(usuarioModuloRepository.findByUsuarioIdAndModuloId(8L, 9L)).thenReturn(Optional.empty());
		when(usuarioModuloRepository.save(any(UsuarioModulo.class))).thenAnswer(invocation -> {
			UsuarioModulo vinculo = invocation.getArgument(0);
			vinculo.setId(40L);
			return vinculo;
		});

		var response = service.atualizar(1L, 8L, 9L, new UsuarioModuloRequest(true));

		ArgumentCaptor<UsuarioModulo> captor = ArgumentCaptor.forClass(UsuarioModulo.class);
		verify(usuarioModuloRepository).save(captor.capture());
		assertThat(captor.getValue().isLiberado()).isTrue();
		assertThat(response.liberado()).isTrue();
		assertThat(response.contratado()).isTrue();
	}

	@Test
	void deveRejeitarLiberacaoDeModuloNaoContratado() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);
		empresa.setAtivo(true);

		Usuario usuario = new Usuario();
		usuario.setId(8L);
		usuario.setEmpresa(empresa);

		Modulo modulo = new Modulo();
		modulo.setId(9L);
		modulo.setCodigo("ESCALA");
		modulo.setAtivo(true);

		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(usuario));
		when(moduloRepository.findById(9L)).thenReturn(Optional.of(modulo));
		when(empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(1L)).thenReturn(Set.of());

		assertThatThrownBy(() -> service.atualizar(1L, 8L, 9L, new UsuarioModuloRequest(true)))
			.isInstanceOf(RegraNegocioException.class);
	}
}
