package br.com.gop.controllers;

import br.com.gop.models.EmpresaModuloResponse;
import br.com.gop.models.OrganizacaoUsuarioRequest;
import br.com.gop.models.OrganizacaoUsuarioResponse;
import br.com.gop.models.UsuarioModuloRequest;
import br.com.gop.models.UsuarioModuloResponse;
import br.com.gop.security.SegurancaContexto;
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

@RestController
@RequestMapping("/v1/org")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class OrganizacaoAdminController {

	private final OrganizacaoUsuarioService organizacaoUsuarioService;
	private final UsuarioModuloService usuarioModuloService;
	private final EmpresaModuloService empresaModuloService;

	public OrganizacaoAdminController(
		OrganizacaoUsuarioService organizacaoUsuarioService,
		UsuarioModuloService usuarioModuloService,
		EmpresaModuloService empresaModuloService
	) {
		this.organizacaoUsuarioService = organizacaoUsuarioService;
		this.usuarioModuloService = usuarioModuloService;
		this.empresaModuloService = empresaModuloService;
	}

	@GetMapping("/usuarios")
	public List<OrganizacaoUsuarioResponse> listarUsuarios() {
		return organizacaoUsuarioService.listar(SegurancaContexto.empresaIdObrigatoria());
	}

	@GetMapping("/usuarios/{usuarioId}")
	public OrganizacaoUsuarioResponse buscarUsuario(@PathVariable Long usuarioId) {
		return organizacaoUsuarioService.buscar(SegurancaContexto.empresaIdObrigatoria(), usuarioId);
	}

	@PostMapping("/usuarios")
	public ResponseEntity<OrganizacaoUsuarioResponse> criarUsuario(
		@Valid @RequestBody OrganizacaoUsuarioRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(organizacaoUsuarioService.criar(SegurancaContexto.empresaIdObrigatoria(), request));
	}

	@PutMapping("/usuarios/{usuarioId}")
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
	public ResponseEntity<Void> desativarUsuario(@PathVariable Long usuarioId) {
		organizacaoUsuarioService.desativar(
			SegurancaContexto.empresaIdObrigatoria(),
			usuarioId,
			SegurancaContexto.usuarioIdAtual()
		);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/modulos")
	@PreAuthorize(
		"hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR') or hasAuthority('EMPRESA_MODULO_LIBERAR')"
	)
	public List<EmpresaModuloResponse> listarModulosContratados() {
		Long empresaId = SegurancaContexto.empresaIdObrigatoria();
		return empresaModuloService.listarPorEmpresa(empresaId)
			.stream()
			.filter(EmpresaModuloResponse::habilitado)
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
}
