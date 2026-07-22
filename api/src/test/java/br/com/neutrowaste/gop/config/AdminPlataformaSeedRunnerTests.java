package br.com.neutrowaste.gop.config;

import br.com.neutrowaste.gop.models.EscopoPapel;
import br.com.neutrowaste.gop.models.Papel;
import br.com.neutrowaste.gop.models.Permissao;
import br.com.neutrowaste.gop.models.Usuario;
import br.com.neutrowaste.gop.repositories.PapelPermissaoRepository;
import br.com.neutrowaste.gop.repositories.PapelRepository;
import br.com.neutrowaste.gop.repositories.PermissaoRepository;
import br.com.neutrowaste.gop.repositories.UsuarioPapelRepository;
import br.com.neutrowaste.gop.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPlataformaSeedRunnerTests {

	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private PapelRepository papelRepository;
	@Mock
	private PermissaoRepository permissaoRepository;
	@Mock
	private PapelPermissaoRepository papelPermissaoRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private UsuarioPapelRepository usuarioPapelRepository;

	private AdminPlataformaSeedProperties properties;
	private AdminPlataformaSeedRunner runner;

	@BeforeEach
	void configurar() {
		properties = new AdminPlataformaSeedProperties();
		properties.setEnabled(true);
		properties.setEmail("admin@neutrowaste.com");
		properties.setSenha("Admin@Gop2026");
		properties.setNome("Admin Plataforma");

		runner = new AdminPlataformaSeedRunner(
			properties,
			passwordEncoder,
			papelRepository,
			permissaoRepository,
			papelPermissaoRepository,
			usuarioRepository,
			usuarioPapelRepository
		);
	}

	@Test
	void deveCriarPapelPermissoesUsuarioEVinculoQuandoAusentes() {
		when(papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull("ADMIN_PLATAFORMA"))
			.thenReturn(Optional.empty());
		when(papelRepository.save(any(Papel.class))).thenAnswer(invocation -> {
			Papel papel = invocation.getArgument(0);
			papel.setId(1L);
			return papel;
		});
		when(permissaoRepository.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.empty());
		when(permissaoRepository.save(any(Permissao.class))).thenAnswer(invocation -> {
			Permissao permissao = invocation.getArgument(0);
			permissao.setId((long) (Math.abs(permissao.getCodigo().hashCode()) % 10_000) + 1L);
			return permissao;
		});
		when(papelPermissaoRepository.existsByPapelIdAndPermissaoId(anyLong(), anyLong()))
			.thenReturn(false);
		when(usuarioRepository.findByEmailIgnoreCase("admin@neutrowaste.com"))
			.thenReturn(Optional.empty());
		when(passwordEncoder.encode("Admin@Gop2026")).thenReturn("hash-bcrypt");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId(10L);
			return usuario;
		});
		when(usuarioPapelRepository.existsVinculoPlataforma(10L, 1L)).thenReturn(false);

		runner.run(new DefaultApplicationArguments());

		ArgumentCaptor<Papel> papelCaptor = ArgumentCaptor.forClass(Papel.class);
		verify(papelRepository).save(papelCaptor.capture());
		assertThat(papelCaptor.getValue().getEscopo()).isEqualTo(EscopoPapel.PLATAFORMA);
		assertThat(papelCaptor.getValue().getEmpresa()).isNull();

		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(usuarioCaptor.capture());
		assertThat(usuarioCaptor.getValue().getEmail()).isEqualTo("admin@neutrowaste.com");
		assertThat(usuarioCaptor.getValue().getSenhaHash()).isEqualTo("hash-bcrypt");
		assertThat(usuarioCaptor.getValue().getEmpresa()).isNull();

		verify(usuarioPapelRepository).save(any());
	}

	@Test
	void naoDeveRecriarQuandoJaExiste() {
		Papel papel = new Papel();
		papel.setId(1L);
		papel.setCodigo("ADMIN_PLATAFORMA");
		papel.setEscopo(EscopoPapel.PLATAFORMA);

		Usuario usuario = new Usuario();
		usuario.setId(10L);
		usuario.setEmail("admin@neutrowaste.com");

		Permissao empresa = new Permissao();
		empresa.setId(2L);
		empresa.setCodigo("PLATAFORMA_EMPRESA_GERENCIAR");
		Permissao licenca = new Permissao();
		licenca.setId(3L);
		licenca.setCodigo("PLATAFORMA_LICENCA_GERENCIAR");

		when(papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull("ADMIN_PLATAFORMA"))
			.thenReturn(Optional.of(papel));
		when(permissaoRepository.findByCodigoIgnoreCase("PLATAFORMA_EMPRESA_GERENCIAR"))
			.thenReturn(Optional.of(empresa));
		when(permissaoRepository.findByCodigoIgnoreCase("PLATAFORMA_LICENCA_GERENCIAR"))
			.thenReturn(Optional.of(licenca));
		when(papelPermissaoRepository.existsByPapelIdAndPermissaoId(1L, 2L)).thenReturn(true);
		when(papelPermissaoRepository.existsByPapelIdAndPermissaoId(1L, 3L)).thenReturn(true);
		when(usuarioRepository.findByEmailIgnoreCase("admin@neutrowaste.com"))
			.thenReturn(Optional.of(usuario));
		when(usuarioPapelRepository.existsVinculoPlataforma(10L, 1L)).thenReturn(true);

		runner.run(new DefaultApplicationArguments());

		verify(papelRepository, never()).save(any());
		verify(usuarioRepository, never()).save(any());
		verify(usuarioPapelRepository, never()).save(any());
		verify(passwordEncoder, never()).encode(anyString());
	}
}
