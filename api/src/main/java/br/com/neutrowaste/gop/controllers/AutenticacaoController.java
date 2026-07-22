package br.com.neutrowaste.gop.controllers;

import br.com.neutrowaste.gop.models.LoginRequest;
import br.com.neutrowaste.gop.models.TokenResponse;
import br.com.neutrowaste.gop.models.UsuarioSessaoResponse;
import br.com.neutrowaste.gop.security.UsuarioTokenPrincipal;
import br.com.neutrowaste.gop.services.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Profile("postgres")
public class AutenticacaoController {

	private final AutenticacaoService autenticacaoService;

	public AutenticacaoController(AutenticacaoService autenticacaoService) {
		this.autenticacaoService = autenticacaoService;
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
			principal.nome(),
			principal.email(),
			principal.papeis(),
			principal.permissoes()
		));
	}
}
