package br.com.gop.controllers;

import br.com.gop.models.MapaOperacionalResponse;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.security.UsuarioTokenPrincipal;
import br.com.gop.services.MapaOperacionalService;
import br.com.gop.services.UsuarioModuloService;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Mapa operacional (RN-001): retrato do dia (ativos + trechos do plano) das
 * Escalas PUBLICADA da empresa. Diferente dos demais recursos operacionais,
 * aqui o funcionário comum também acessa quando o módulo MONITORAMENTO está
 * liberado para o seu usuário — não é restrito a ADMIN_EMPRESA.
 */
@RestController
@RequestMapping("/v1/org/mapa-operacional")
@Profile("postgres")
@PreAuthorize("isAuthenticated()")
public class MapaOperacionalController {

	private static final String MODULO_MONITORAMENTO = "MONITORAMENTO";

	private final MapaOperacionalService mapaOperacionalService;
	private final UsuarioModuloService usuarioModuloService;

	public MapaOperacionalController(
		MapaOperacionalService mapaOperacionalService,
		UsuarioModuloService usuarioModuloService
	) {
		this.mapaOperacionalService = mapaOperacionalService;
		this.usuarioModuloService = usuarioModuloService;
	}

	@GetMapping
	public MapaOperacionalResponse obter(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
	) {
		UsuarioTokenPrincipal principal = garantirAcesso();
		return mapaOperacionalService.obter(principal.empresaId(), data);
	}

	private UsuarioTokenPrincipal garantirAcesso() {
		UsuarioTokenPrincipal principal = SegurancaContexto.principalAtual();
		SegurancaContexto.empresaIdObrigatoria();
		boolean acessoAdministrativo = principal.papeis().contains("ADMIN_EMPRESA")
			|| principal.permissoes().contains("EMPRESA_USUARIO_GERENCIAR");
		if (!acessoAdministrativo
			&& !usuarioModuloService.possuiModuloLiberado(principal.usuarioId(), MODULO_MONITORAMENTO)) {
			throw new AccessDeniedException("Módulo Monitoramento não liberado para este usuário");
		}
		return principal;
	}
}
