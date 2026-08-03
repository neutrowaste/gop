package br.com.gop.security;

import br.com.gop.config.SecurityConfig;
import br.com.gop.services.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
		this.jwtService = jwtService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			UsuarioTokenPrincipal principal = jwtService.validar(
				authorization.substring(BEARER_PREFIX.length()).strip()
			);
			var authorities = principal.authorities().stream()
				.map(SimpleGrantedAuthority::new)
				.toList();
			var authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				authorities
			);
			authentication.setDetails(
				new WebAuthenticationDetailsSource().buildDetails(request)
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (JwtService.JwtInvalidoException exception) {
			SecurityContextHolder.clearContext();
			SecurityConfig.escreverProblema(
				response,
				objectMapper,
				HttpStatus.UNAUTHORIZED,
				"Não autorizado",
				exception.getMessage()
			);
			return;
		}
	}
}
