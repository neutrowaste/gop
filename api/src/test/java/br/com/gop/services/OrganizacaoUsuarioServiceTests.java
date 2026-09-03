package br.com.gop.services;

import br.com.gop.config.CatalogoPlataformaSeedRunner;
import br.com.gop.models.Empresa;
import br.com.gop.models.EscopoPapel;
import br.com.gop.models.OrganizacaoUsuarioRequest;
import br.com.gop.models.Papel;
import br.com.gop.models.Usuario;
import br.com.gop.models.UsuarioPapel;
import br.com.gop.repositories.PapelRepository;
import br.com.gop.repositories.UsuarioPapelRepository;
import br.com.gop.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizacaoUsuarioServiceTests {

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

	private OrganizacaoUsuarioService service;

	@BeforeEach
	void configurar() {
		service = new OrganizacaoUsuarioService(
			empresaService,
			usuarioRepository,
			papelRepository,
			usuarioPapelRepository,
			passwordEncoder,
			auditoriaService
		);
	}

	@Test
	void deveCriarUsuarioDaEmpresaSemAdmin() {
		Empresa empresa = empresaAtiva(11L);
		when(empresaService.buscarEntidade(11L)).thenReturn(empresa);
		when(usuarioRepository.existsByEmailIgnoreCase("ops@cliente.com")).thenReturn(false);
		when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId(90L);
			return usuario;
		});
		when(usuarioPapelRepository.findPapeisAtivosByUsuarioId(90L)).thenReturn(List.of());
		Papel papelUsuario = papelUsuario();
		when(papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull(
			CatalogoPlataformaSeedRunner.CODIGO_PAPEL_USUARIO_EMPRESA
		)).thenReturn(Optional.of(papelUsuario));
		when(usuarioPapelRepository.existsVinculoEmpresa(90L, 4L, 11L)).thenReturn(false);

		var response = service.criar(
			11L,
			new OrganizacaoUsuarioRequest(" Ops@Cliente.com ", "Senha@123", " Operador ", true, false)
		);

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(captor.capture());
		assertThat(captor.getValue().getEmail()).isEqualTo("ops@cliente.com");
		assertThat(captor.getValue().getEmpresa()).isSameAs(empresa);
		verify(usuarioPapelRepository).save(any(UsuarioPapel.class));
		assertThat(response.administradorEmpresa()).isFalse();
	}

	@Test
	void deveCriarUsuarioComoAdminEmpresa() {
		Empresa empresa = empresaAtiva(11L);
		Papel papel = papelAdmin();
		when(empresaService.buscarEntidade(11L)).thenReturn(empresa);
		when(usuarioRepository.existsByEmailIgnoreCase("admin@cliente.com")).thenReturn(false);
		when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
		when(papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull(
			CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA
		)).thenReturn(Optional.of(papel));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId(91L);
			return usuario;
		});
		when(usuarioPapelRepository.existsVinculoEmpresa(91L, 3L, 11L)).thenReturn(false);
		when(usuarioPapelRepository.findPapeisAtivosByUsuarioId(91L)).thenAnswer(invocation -> {
			UsuarioPapel vinculo = new UsuarioPapel();
			vinculo.setPapel(papel);
			return List.of(vinculo);
		});

		var response = service.criar(
			11L,
			new OrganizacaoUsuarioRequest("admin@cliente.com", "Senha@123", "Admin", true, true)
		);

		verify(usuarioPapelRepository).save(any(UsuarioPapel.class));
		assertThat(response.administradorEmpresa()).isTrue();
	}

	@Test
	void naoDeveDesativarProprioUsuario() {
		assertThatThrownBy(() -> service.desativar(11L, 5L, 5L))
			.isInstanceOf(RegraNegocioException.class);
		verify(usuarioRepository, never()).findByIdAndEmpresaId(any(), any());
	}

	@Test
	void deveRejeitarEmailDuplicado() {
		when(empresaService.buscarEntidade(11L)).thenReturn(empresaAtiva(11L));
		when(usuarioRepository.existsByEmailIgnoreCase("ops@cliente.com")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(
			11L,
			new OrganizacaoUsuarioRequest("ops@cliente.com", "Senha@123", "Ops", true, false)
		)).isInstanceOf(ConflitoNegocioException.class);
	}

	private Empresa empresaAtiva(Long id) {
		Empresa empresa = new Empresa();
		empresa.setId(id);
		empresa.setAtivo(true);
		return empresa;
	}

	private Papel papelUsuario() {
		Papel papel = new Papel();
		papel.setId(4L);
		papel.setCodigo(CatalogoPlataformaSeedRunner.CODIGO_PAPEL_USUARIO_EMPRESA);
		papel.setEscopo(EscopoPapel.OPERACIONAL);
		papel.setAtivo(true);
		return papel;
	}

	private Papel papelAdmin() {
		Papel papel = new Papel();
		papel.setId(3L);
		papel.setCodigo(CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA);
		papel.setEscopo(EscopoPapel.ORGANIZACAO);
		papel.setAtivo(true);
		return papel;
	}
}
