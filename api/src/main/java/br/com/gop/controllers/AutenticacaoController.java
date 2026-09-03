package br.com.gop.controllers;

import br.com.gop.models.LoginRequest;
import br.com.gop.models.TokenResponse;
import br.com.gop.models.UsuarioAcessoMenuResponse;
import br.com.gop.models.UsuarioModuloResponse;
import br.com.gop.models.UsuarioSessaoResponse;
import br.com.gop.security.UsuarioTokenPrincipal;
import br.com.gop.services.AcessoMenuService;
import br.com.gop.services.AutenticacaoService;
import br.com.gop.services.RegraNegocioException;
import br.com.gop.services.UsuarioModuloService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth")
@Profile("postgres")
public class AutenticacaoController {

	private final AutenticacaoService autenticacaoService;
	private final UsuarioModuloService usuarioModuloService;
	private final AcessoMenuService acessoMenuService;

	public AutenticacaoController(
		AutenticacaoService autenticacaoService,
		UsuarioModuloService usuarioModuloService,
		AcessoMenuService acessoMenuService
	) {
		this.autenticacaoService = autenticacaoService;
		this.usuarioModuloService = usuarioModuloService;
		this.acessoMenuService = acessoMenuService;
	}

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(autenticacaoService.autenticar(request));
	}

	@GetMapping("/me")
	public ResponseEntity<UsuarioSessaoResponse> me(
		@AuthenticationPrincipal UsuarioTokenPrincipal principal
	) {
		return ResponseEntity.ok(new UsuarioSessaoResponse(
			principal.usuarioId(),
			principal.empresaId(),
			principal.empresaNome(),
			principal.empresaCodigo(),
			principal.nome(),
			principal.email(),
			principal.papeis(),
			principal.permissoes()
		));
	}

	@GetMapping("/me/modulos")
	public ResponseEntity<List<UsuarioModuloResponse>> meusModulos(
		@AuthenticationPrincipal UsuarioTokenPrincipal principal
	) {
		if (principal.empresaId() == null) {
			throw new RegraNegocioException("Usuário sem empresa vinculada");
		}
		return ResponseEntity.ok(
			usuarioModuloService.listarLiberados(principal.empresaId(), principal.usuarioId())
		);
	}

	@GetMapping("/me/acessos")
	public ResponseEntity<List<UsuarioAcessoMenuResponse>> meusAcessos(
		@AuthenticationPrincipal UsuarioTokenPrincipal principal
	) {
		if (principal.empresaId() == null) {
			throw new RegraNegocioException("Usuário sem empresa vinculada");
		}
		boolean adminEmpresa = principal.papeis().stream()
			.anyMatch(papel -> "ADMIN_EMPRESA".equalsIgnoreCase(papel));
		return ResponseEntity.ok(
			acessoMenuService.listarMeus(principal.empresaId(), principal.usuarioId(), adminEmpresa)
		);
	}
}
