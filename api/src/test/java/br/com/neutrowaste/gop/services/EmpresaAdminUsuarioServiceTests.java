package br.com.neutrowaste.gop.services;

import br.com.neutrowaste.gop.config.CatalogoPlataformaSeedRunner;
import br.com.neutrowaste.gop.models.Empresa;
import br.com.neutrowaste.gop.models.EmpresaAdminRequest;
import br.com.neutrowaste.gop.models.EscopoPapel;
import br.com.neutrowaste.gop.models.Papel;
import br.com.neutrowaste.gop.models.Usuario;
import br.com.neutrowaste.gop.models.UsuarioPapel;
import br.com.neutrowaste.gop.repositories.PapelRepository;
import br.com.neutrowaste.gop.repositories.UsuarioPapelRepository;
import br.com.neutrowaste.gop.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaAdminUsuarioServiceTests {

	@Mock
	private EmpresaService empresaService;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PapelRepository papelRepository;

	@Mock
	private UsuarioPapelRepository usuarioPapelRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuditoriaService auditoriaService;

	private EmpresaAdminUsuarioService service;

	@BeforeEach
	void configurar() {
		service = new EmpresaAdminUsuarioService(
			empresaService,
			usuarioRepository,
			papelRepository,
			usuarioPapelRepository,
			passwordEncoder,
			auditoriaService
		);
	}

	@Test
	void deveCriarAdminDaEmpresa() {
		Empresa empresa = new Empresa();
		empresa.setId(7L);
		empresa.setAtivo(true);

		Papel papel = new Papel();
		papel.setId(3L);
		papel.setCodigo(CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA);
		papel.setEscopo(EscopoPapel.ORGANIZACAO);
		papel.setAtivo(true);

		when(empresaService.buscarEntidade(7L)).thenReturn(empresa);
		when(usuarioRepository.existsByEmailIgnoreCase("admin@cliente.com")).thenReturn(false);
		when(papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull(
			CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA
		)).thenReturn(Optional.of(papel));
		when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId(44L);
			return usuario;
		});

		var response = service.criar(
			7L,
			new EmpresaAdminRequest(" Admin@Cliente.com ", "Senha@123", " Admin Cliente ")
		);

		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(usuarioCaptor.capture());
		assertThat(usuarioCaptor.getValue().getEmail()).isEqualTo("admin@cliente.com");
		assertThat(usuarioCaptor.getValue().getNome()).isEqualTo("Admin Cliente");
		assertThat(usuarioCaptor.getValue().getEmpresa()).isSameAs(empresa);

		ArgumentCaptor<UsuarioPapel> vinculoCaptor = ArgumentCaptor.forClass(UsuarioPapel.class);
		verify(usuarioPapelRepository).save(vinculoCaptor.capture());
		assertThat(vinculoCaptor.getValue().getPapel()).isSameAs(papel);
		assertThat(vinculoCaptor.getValue().getEmpresa()).isSameAs(empresa);
		assertThat(response.id()).isEqualTo(44L);
	}

	@Test
	void deveRejeitarEmailDuplicado() {
		Empresa empresa = new Empresa();
		empresa.setId(7L);
		empresa.setAtivo(true);
		when(empresaService.buscarEntidade(7L)).thenReturn(empresa);
		when(usuarioRepository.existsByEmailIgnoreCase("admin@cliente.com")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(
			7L,
			new EmpresaAdminRequest("admin@cliente.com", "Senha@123", "Admin")
		)).isInstanceOf(ConflitoNegocioException.class);

		verify(usuarioRepository, never()).save(any());
	}
}
