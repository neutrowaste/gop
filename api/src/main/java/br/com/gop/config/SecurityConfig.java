package br.com.gop.config;

import br.com.gop.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		ObjectMapper objectMapper
	) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/actuator/health",
					"/actuator/info",
					"/v1/health",
					"/v1/auth/login",
					"/v1/auth/login/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint((request, response, exception) ->
					escreverProblema(
						response,
						objectMapper,
						HttpStatus.UNAUTHORIZED,
						"Não autorizado",
						"Autenticação obrigatória"
					)
				)
				.accessDeniedHandler((request, response, exception) ->
					escreverProblema(response, objectMapper, HttpStatus.FORBIDDEN, "Acesso negado", "Acesso negado")
				)
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * Falhas de autenticação/autorização são resolvidas pelo filtro de segurança
	 * antes de chegar ao DispatcherServlet, então não passam pelo ApiExceptionHandler.
	 * Escrevemos o mesmo formato ProblemDetail aqui para o frontend sempre ter um
	 * campo "detail" e conseguir distinguir sessão expirada de outros erros.
	 */
	public static void escreverProblema(
		jakarta.servlet.http.HttpServletResponse response,
		ObjectMapper objectMapper,
		HttpStatus status,
		String titulo,
		String detalhe
	) throws java.io.IOException {
		ProblemDetail problema = ProblemDetail.forStatus(status);
		problema.setTitle(titulo);
		problema.setDetail(detalhe);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(problema));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Profile("postgres")
	AuthenticationManager authenticationManager(
		UserDetailsService userDetailsService,
		PasswordEncoder passwordEncoder
	) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of("http://localhost:4200"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
