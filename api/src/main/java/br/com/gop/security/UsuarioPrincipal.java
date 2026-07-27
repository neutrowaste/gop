package br.com.gop.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

public record UsuarioPrincipal(
	Long usuarioId,
	Long empresaId,
	String empresaNome,
	String empresaCodigo,
	String nome,
	String username,
	String password,
	boolean ativo,
	Set<String> papeis,
	Set<String> permissoes,
	Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return ativo;
	}
}
