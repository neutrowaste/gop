package br.com.gop.security;

import br.com.gop.services.RegraNegocioException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SegurancaContexto {

	private SegurancaContexto() {
	}

	public static UsuarioTokenPrincipal principalAtual() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioTokenPrincipal principal)) {
			throw new AccessDeniedException("Autenticação obrigatória");
		}
		return principal;
	}

	public static Long empresaIdObrigatoria() {
		Long empresaId = principalAtual().empresaId();
		if (empresaId == null) {
			throw new RegraNegocioException("Operação disponível apenas para usuários vinculados a uma empresa");
		}
		return empresaId;
	}

	public static Long usuarioIdAtual() {
		return principalAtual().usuarioId();
	}
}
