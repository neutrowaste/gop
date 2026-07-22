package br.com.neutrowaste.gop.services;

import br.com.neutrowaste.gop.security.UsuarioPrincipal;
import br.com.neutrowaste.gop.security.UsuarioTokenPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final String issuer;
	private final Duration accessTokenTtl;
	private final byte[] secret;

	@Autowired
	public JwtService(
		ObjectMapper objectMapper,
		@Value("${security.jwt.issuer}") String issuer,
		@Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl,
		@Value("${security.jwt.secret}") String secret
	) {
		this(objectMapper, Clock.systemUTC(), issuer, accessTokenTtl, secret);
	}

	JwtService(
		ObjectMapper objectMapper,
		Clock clock,
		String issuer,
		Duration accessTokenTtl,
		String secret
	) {
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalArgumentException("security.jwt.secret deve ter ao menos 32 bytes");
		}
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.issuer = issuer;
		this.accessTokenTtl = accessTokenTtl;
		this.secret = secretBytes.clone();
	}

	public TokenEmitido emitir(UsuarioPrincipal principal) {
		Instant emitidoEm = clock.instant();
		Instant expiraEm = emitidoEm.plus(accessTokenTtl);
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("iss", issuer);
		claims.put("sub", principal.getUsername());
		claims.put("iat", emitidoEm.getEpochSecond());
		claims.put("exp", expiraEm.getEpochSecond());
		claims.put("uid", principal.usuarioId());
		claims.put("nome", principal.nome());
		claims.put("papeis", principal.papeis());
		claims.put("permissoes", principal.permissoes());
		claims.put(
			"authorities",
			principal.getAuthorities().stream().map(authority -> authority.getAuthority()).toList()
		);
		if (principal.empresaId() != null) {
			claims.put("empresa_id", principal.empresaId());
		}

		String header = codificarJson(Map.of("alg", "HS256", "typ", "JWT"));
		String payload = codificarJson(claims);
		String conteudoAssinado = header + "." + payload;
		String assinatura = BASE64_URL_ENCODER.encodeToString(assinar(conteudoAssinado));
		String token = conteudoAssinado + "." + assinatura;
		return new TokenEmitido(token, expiraEm);
	}

	public UsuarioTokenPrincipal validar(String token) {
		try {
			String[] partes = token.split("\\.", -1);
			if (partes.length != 3) {
				throw new JwtInvalidoException();
			}

			byte[] assinaturaRecebida = BASE64_URL_DECODER.decode(partes[2]);
			byte[] assinaturaEsperada = assinar(partes[0] + "." + partes[1]);
			if (!MessageDigest.isEqual(assinaturaRecebida, assinaturaEsperada)) {
				throw new JwtInvalidoException();
			}

			Map<String, Object> header = objectMapper.readValue(
				BASE64_URL_DECODER.decode(partes[0]),
				MAP_TYPE
			);
			if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
				throw new JwtInvalidoException();
			}

			Map<String, Object> claims = objectMapper.readValue(
				BASE64_URL_DECODER.decode(partes[1]),
				MAP_TYPE
			);
			validarClaims(claims);
			return new UsuarioTokenPrincipal(
				numeroObrigatorio(claims, "uid"),
				numeroOpcional(claims, "empresa_id"),
				textoObrigatorio(claims, "nome"),
				textoObrigatorio(claims, "sub"),
				listaDeTextos(claims, "papeis"),
				listaDeTextos(claims, "permissoes"),
				listaDeTextos(claims, "authorities")
			);
		} catch (IllegalArgumentException | IOException exception) {
			throw new JwtInvalidoException();
		}
	}

	private void validarClaims(Map<String, Object> claims) {
		if (!issuer.equals(claims.get("iss"))) {
			throw new JwtInvalidoException();
		}
		long agora = clock.instant().getEpochSecond();
		long expiracao = numeroObrigatorio(claims, "exp");
		long emitidoEm = numeroObrigatorio(claims, "iat");
		if (expiracao <= agora || emitidoEm > agora + 60) {
			throw new JwtInvalidoException();
		}
	}

	private String codificarJson(Object valor) {
		try {
			return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(valor));
		} catch (IOException exception) {
			throw new IllegalStateException("Não foi possível emitir o token", exception);
		}
	}

	private byte[] assinar(String conteudo) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("HMAC-SHA256 indisponível", exception);
		}
	}

	private Long numeroObrigatorio(Map<String, Object> claims, String nome) {
		Object valor = claims.get(nome);
		if (valor instanceof Number numero) {
			return numero.longValue();
		}
		throw new JwtInvalidoException();
	}

	private Long numeroOpcional(Map<String, Object> claims, String nome) {
		return claims.containsKey(nome) ? numeroObrigatorio(claims, nome) : null;
	}

	private String textoObrigatorio(Map<String, Object> claims, String nome) {
		Object valor = claims.get(nome);
		if (valor instanceof String texto && !texto.isBlank()) {
			return texto;
		}
		throw new JwtInvalidoException();
	}

	private List<String> listaDeTextos(Map<String, Object> claims, String nome) {
		Object valor = claims.get(nome);
		if (!(valor instanceof List<?> lista)
			|| lista.stream().anyMatch(item -> !(item instanceof String))) {
			throw new JwtInvalidoException();
		}
		return lista.stream().map(String.class::cast).toList();
	}

	public record TokenEmitido(String valor, Instant expiraEm) {
	}

	public static class JwtInvalidoException extends RuntimeException {

		public JwtInvalidoException() {
			super("Token inválido ou expirado");
		}
	}
}
