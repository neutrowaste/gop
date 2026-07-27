package br.com.gop.security;

import java.util.List;

public record UsuarioTokenPrincipal(
	Long usuarioId,
	Long empresaId,
	String empresaNome,
	String empresaCodigo,
	String nome,
	String email,
	List<String> papeis,
	List<String> permissoes,
	List<String> authorities
) {
}
