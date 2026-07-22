package br.com.neutrowaste.gop.services;

import br.com.neutrowaste.gop.security.UsuarioPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

	private static final String SECRET = "segredo-de-teste-com-mais-de-32-bytes";
	private static final Instant AGORA = Instant.parse("2026-07-21T14:00:00Z");

	@Test
	void deveEmitirEValidarTokenComTenantERbac() {
		JwtService service = service(AGORA);
		UsuarioPrincipal usuario = usuario();

		JwtService.TokenEmitido token = service.emitir(usuario);
		var principal = service.validar(token.valor());

		assertThat(principal.usuarioId()).isEqualTo(10L);
		assertThat(principal.empresaId()).isEqualTo(20L);
		assertThat(principal.papeis()).containsExactly("SUPERVISOR");
		assertThat(principal.permissoes()).containsExactly("OPERACAO_VISUALIZAR");
		assertThat(principal.authorities())
			.containsExactlyInAnyOrder("ROLE_SUPERVISOR", "OPERACAO_VISUALIZAR");
		assertThat(token.expiraEm()).isEqualTo(AGORA.plusSeconds(900));
	}

	@Test
	void deveRejeitarTokenAdulterado() {
		JwtService service = service(AGORA);
		String token = service.emitir(usuario()).valor();
		String adulterado = token.substring(0, token.length() - 1)
			+ (token.endsWith("a") ? "b" : "a");

		assertThatThrownBy(() -> service.validar(adulterado))
			.isInstanceOf(JwtService.JwtInvalidoException.class);
	}

	@Test
	void deveRejeitarTokenExpirado() {
		String token = service(AGORA).emitir(usuario()).valor();

		assertThatThrownBy(() -> service(AGORA.plusSeconds(901)).validar(token))
			.isInstanceOf(JwtService.JwtInvalidoException.class);
	}

	private JwtService service(Instant agora) {
		return new JwtService(
			new ObjectMapper(),
			Clock.fixed(agora, ZoneOffset.UTC),
			"neutrowaste-gop",
			Duration.ofMinutes(15),
			SECRET
		);
	}

	private UsuarioPrincipal usuario() {
		return new UsuarioPrincipal(
			10L,
			20L,
			"Supervisor",
			"supervisor@cliente.com",
			"hash",
			true,
			Set.of("SUPERVISOR"),
			Set.of("OPERACAO_VISUALIZAR"),
			Set.of(
				new SimpleGrantedAuthority("ROLE_SUPERVISOR"),
				new SimpleGrantedAuthority("OPERACAO_VISUALIZAR")
			)
		);
	}
}
