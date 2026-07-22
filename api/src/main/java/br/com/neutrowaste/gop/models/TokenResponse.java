package br.com.neutrowaste.gop.models;

import java.time.Instant;
import java.util.Set;

public record TokenResponse(
	String accessToken,
	String tokenType,
	Instant expiraEm,
	Long usuarioId,
	Long empresaId,
	String nome,
	String email,
	Set<String> papeis,
	Set<String> permissoes
) {
}
