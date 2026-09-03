package br.com.gop.controllers;

import br.com.gop.models.EmpresaModuloResponse;
import br.com.gop.models.OrganizacaoUsuarioRequest;
import br.com.gop.models.OrganizacaoUsuarioResponse;
import br.com.gop.models.TetoAtivosResponse;
import br.com.gop.models.UsuarioAcessoMenuRequest;
import br.com.gop.models.UsuarioAcessoMenuResponse;
import br.com.gop.models.UsuarioModuloRequest;
import br.com.gop.models.UsuarioModuloResponse;
import br.com.gop.security.OrgAuth;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.AcessoMenuService;
import br.com.gop.services.ControleTetoAtivosService;
import br.com.gop.services.EmpresaModuloService;
import br.com.gop.services.OrganizacaoUsuarioService;
import br.com.gop.services.UsuarioModuloService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/org")
@Profile("postgres")
@PreAuthorize("isAuthenticated()")
public class OrganizacaoAdminController {

	private final OrganizacaoUsuarioService organizacaoUsuarioService;
	private final UsuarioModuloService usuarioModuloService;
	private final AcessoMenuService acessoMenuService;
	private final EmpresaModuloService empresaModuloService;
	private final ControleTetoAtivosService controleTetoAtivosService;

	public OrganizacaoAdminController(
		OrganizacaoUsuarioService organizacaoUsuarioService,
		UsuarioModuloService usuarioModuloService,
		AcessoMenuService acessoMenuService,
		EmpresaModuloService empresaModuloService,
		ControleTetoAtivosService controleTetoAtivosService
	) {
		this.organizacaoUsuarioService = organizacaoUsuarioService;
		this.usuarioModuloService = usuarioModuloService;
		this.acessoMenuService = acessoMenuService;
		this.empresaModuloService = empresaModuloService;
		this.controleTetoAtivosService = controleTetoAtivosService;
	}

	@GetMapping("/usuarios")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public List<OrganizacaoUsuarioResponse> listarUsuarios() {
		return organizacaoUsuarioService.listar(SegurancaContexto.empresaIdObrigatoria());
	}

	@GetMapping("/teto-ativos")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public TetoAtivosResponse consultarTeto() {
		return controleTetoAtivosService.consultar(SegurancaContexto.empresaIdObrigatoria());
	}

	@GetMapping("/usuarios/{usuarioId}")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public OrganizacaoUsuarioResponse buscarUsuario(@PathVariable Long usuarioId) {
		return organizacaoUsuarioService.buscar(SegurancaContexto.empresaIdObrigatoria(), usuarioId);
	}

	@PostMapping("/usuarios")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public ResponseEntity<OrganizacaoUsuarioResponse> criarUsuario(
		@Valid @RequestBody OrganizacaoUsuarioRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(organizacaoUsuarioService.criar(SegurancaContexto.empresaIdObrigatoria(), request));
	}

	@PutMapping("/usuarios/{usuarioId}")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public OrganizacaoUsuarioResponse atualizarUsuario(
		@PathVariable Long usuarioId,
		@Valid @RequestBody OrganizacaoUsuarioRequest request
	) {
		return organizacaoUsuarioService.atualizar(
			SegurancaContexto.empresaIdObrigatoria(),
			usuarioId,
			request
		);
	}

	@DeleteMapping("/usuarios/{usuarioId}")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public ResponseEntity<Void> desativarUsuario(@PathVariable Long usuarioId) {
		organizacaoUsuarioService.desativar(
			SegurancaContexto.empresaIdObrigatoria(),
			usuarioId,
			SegurancaContexto.usuarioIdAtual()
		);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/modulos")
	@PreAuthorize(OrgAuth.DA_EMPRESA)
	public List<EmpresaModuloResponse> listarModulosContratados() {
		Long empresaId = SegurancaContexto.empresaIdObrigatoria();
		List<EmpresaModuloResponse> contratados = empresaModuloService.listarPorEmpresa(empresaId)
			.stream()
			.filter(EmpresaModuloResponse::habilitado)
			.toList();
		var principal = SegurancaContexto.principalAtual();
		boolean admin = principal.papeis().stream()
			.anyMatch(papel -> "ADMIN_EMPRESA".equalsIgnoreCase(papel));
		if (admin) {
			return contratados;
		}
		Set<String> liberados = usuarioModuloService
			.listarLiberados(empresaId, principal.usuarioId())
			.stream()
			.map(item -> item.codigo().toUpperCase(Locale.ROOT))
			.collect(Collectors.toSet());
		return contratados.stream()
			.filter(item -> liberados.contains(item.codigo().toUpperCase(Locale.ROOT)))
			.toList();
	}

	@GetMapping("/usuarios/{usuarioId}/modulos")
	@PreAuthorize(
		"hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_MODULO_LIBERAR')"
	)
	public List<UsuarioModuloResponse> listarModulosDoUsuario(@PathVariable Long usuarioId) {
		return usuarioModuloService.listarPorUsuario(
			SegurancaContexto.empresaIdObrigatoria(),
			usuarioId
		);
	}

	@PutMapping("/usuarios/{usuarioId}/modulos/{moduloId}")
	@PreAuthorize(
		"hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_MODULO_LIBERAR')"
	)
	public UsuarioModuloResponse atualizarModuloDoUsuario(
		@PathVariable Long usuarioId,
		@PathVariable Long moduloId,
		@Valid @RequestBody UsuarioModuloRequest request
	) {
		return usuarioModuloService.atualizar(
			SegurancaContexto.empresaIdObrigatoria(),
			usuarioId,
			moduloId,
			request
		);
	}

	@GetMapping("/usuarios/{usuarioId}/acessos")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public List<UsuarioAcessoMenuResponse> listarAcessosDoUsuario(@PathVariable Long usuarioId) {
		return acessoMenuService.listarPorUsuario(
			SegurancaContexto.empresaIdObrigatoria(),
			usuarioId
		);
	}

	@PutMapping("/usuarios/{usuarioId}/acessos/{codigo}")
	@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
	public UsuarioAcessoMenuResponse atualizarAcessoDoUsuario(
		@PathVariable Long usuarioId,
		@PathVariable String codigo,
		@Valid @RequestBody UsuarioAcessoMenuRequest request
	) {
		return acessoMenuService.atualizar(
			SegurancaContexto.empresaIdObrigatoria(),
			usuarioId,
			codigo,
			request
		);
	}
}
