package br.com.neutrowaste.gop.services;

import br.com.neutrowaste.gop.models.LoginRequest;
import br.com.neutrowaste.gop.models.TokenResponse;
import br.com.neutrowaste.gop.security.UsuarioPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@Profile("postgres")
public class AutenticacaoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutenticacaoService.class);

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AutenticacaoService(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public TokenResponse autenticar(LoginRequest request) {
		try {
			Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(
					request.email().strip(),
					request.senha()
				)
			);
			UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
			JwtService.TokenEmitido token = jwtService.emitir(principal);
			LOGGER.info(
				"Autenticação concluída: usuarioId={}, empresaId={}, papeis={}",
				principal.usuarioId(),
				principal.empresaId(),
				principal.papeis()
			);
			return new TokenResponse(
				token.valor(),
				"Bearer",
				token.expiraEm(),
				principal.usuarioId(),
				principal.empresaId(),
				principal.nome(),
				principal.getUsername(),
				principal.papeis(),
				principal.permissoes()
			);
		} catch (AuthenticationException exception) {
			LOGGER.warn("Falha de autenticação para o email={}", request.email().strip());
			throw exception;
		}
	}
}
