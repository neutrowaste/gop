package br.com.gop.services;

import br.com.gop.config.CatalogoPlataformaSeedRunner;
import br.com.gop.models.AcessoMenu;
import br.com.gop.models.Empresa;
import br.com.gop.models.Papel;
import br.com.gop.models.Usuario;
import br.com.gop.models.UsuarioAcessoMenu;
import br.com.gop.models.UsuarioAcessoMenuRequest;
import br.com.gop.models.UsuarioPapel;
import br.com.gop.repositories.UsuarioAcessoMenuRepository;
import br.com.gop.repositories.UsuarioPapelRepository;
import br.com.gop.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcessoMenuServiceTests {

	@Mock
	private EmpresaService empresaService;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioPapelRepository usuarioPapelRepository;

	@Mock
	private UsuarioAcessoMenuRepository usuarioAcessoMenuRepository;

	@Mock
	private AuditoriaService auditoriaService;

	private AcessoMenuService service;

	@BeforeEach
	void configurar() {
		service = new AcessoMenuService(
			empresaService,
			usuarioRepository,
			usuarioPapelRepository,
			usuarioAcessoMenuRepository,
			auditoriaService
		);
	}

	@Test
	void deveListarCatalogoSemLiberacaoParaUsuarioComum() {
		Empresa empresa = empresaAtiva();
		Usuario usuario = usuarioDaEmpresa(empresa, 8L);
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(usuario));
		when(usuarioPapelRepository.findPapeisAtivosByUsuarioId(8L)).thenReturn(List.of());
		when(usuarioAcessoMenuRepository.findByUsuarioId(8L)).thenReturn(List.of());

		var resposta = service.listarPorUsuario(1L, 8L);

		assertThat(resposta).hasSize(AcessoMenu.catalogo().size());
		assertThat(resposta).allMatch(item -> !item.liberado());
		assertThat(resposta).extracting(item -> item.codigo())
			.containsExactly("SETORES", "TRECHOS", "COLABORADORES", "ESCALAS", "OCORRENCIAS");
	}

	@Test
	void deveMarcarTodosLiberadosParaAdminEmpresa() {
		Empresa empresa = empresaAtiva();
		Usuario usuario = usuarioDaEmpresa(empresa, 8L);
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(usuario));
		when(usuarioPapelRepository.findPapeisAtivosByUsuarioId(8L)).thenReturn(List.of(vinculoAdmin()));
		when(usuarioAcessoMenuRepository.findByUsuarioId(8L)).thenReturn(List.of());

		var resposta = service.listarPorUsuario(1L, 8L);

		assertThat(resposta).allMatch(item -> item.liberado());
	}

	@Test
	void deveLiberarAcessoDeMenu() {
		Empresa empresa = empresaAtiva();
		Usuario usuario = usuarioDaEmpresa(empresa, 8L);
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(usuario));
		when(usuarioPapelRepository.findPapeisAtivosByUsuarioId(8L)).thenReturn(List.of());
		when(usuarioAcessoMenuRepository.findByUsuarioIdAndCodigoIgnoreCase(8L, "SETORES"))
			.thenReturn(Optional.empty());
		when(usuarioAcessoMenuRepository.save(any(UsuarioAcessoMenu.class))).thenAnswer(invocation -> {
			UsuarioAcessoMenu vinculo = invocation.getArgument(0);
			vinculo.setId(21L);
			return vinculo;
		});

		var response = service.atualizar(1L, 8L, "setores", new UsuarioAcessoMenuRequest(true));

		ArgumentCaptor<UsuarioAcessoMenu> captor = ArgumentCaptor.forClass(UsuarioAcessoMenu.class);
		verify(usuarioAcessoMenuRepository).save(captor.capture());
		assertThat(captor.getValue().isLiberado()).isTrue();
		assertThat(captor.getValue().getCodigo()).isEqualTo("SETORES");
		assertThat(response.liberado()).isTrue();
		assertThat(response.codigo()).isEqualTo("SETORES");
	}

	@Test
	void naoDeveAlterarAcessoDeAdminEmpresa() {
		Empresa empresa = empresaAtiva();
		Usuario usuario = usuarioDaEmpresa(empresa, 8L);
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(usuario));
		when(usuarioPapelRepository.findPapeisAtivosByUsuarioId(8L)).thenReturn(List.of(vinculoAdmin()));

		assertThatThrownBy(() -> service.atualizar(1L, 8L, "SETORES", new UsuarioAcessoMenuRequest(true)))
			.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void deveRejeitarCodigoInvalido() {
		Empresa empresa = empresaAtiva();
		Usuario usuario = usuarioDaEmpresa(empresa, 8L);
		when(empresaService.buscarEntidade(1L)).thenReturn(empresa);
		when(usuarioRepository.findByIdAndEmpresaId(8L, 1L)).thenReturn(Optional.of(usuario));

		assertThatThrownBy(() -> service.atualizar(1L, 8L, "USUARIOS", new UsuarioAcessoMenuRequest(true)))
			.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	private Empresa empresaAtiva() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);
		empresa.setAtivo(true);
		return empresa;
	}

	private Usuario usuarioDaEmpresa(Empresa empresa, Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setEmpresa(empresa);
		return usuario;
	}

	private UsuarioPapel vinculoAdmin() {
		Papel papel = new Papel();
		papel.setCodigo(CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA);
		UsuarioPapel vinculo = new UsuarioPapel();
		vinculo.setPapel(papel);
		return vinculo;
	}
}
