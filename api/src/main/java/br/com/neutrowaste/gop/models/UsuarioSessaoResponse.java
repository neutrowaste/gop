package br.com.neutrowaste.gop.models;

import java.util.List;

public record UsuarioSessaoResponse(
	Long usuarioId,
	Long empresaId,
	String nome,
	String email,
	List<String> papeis,
	List<String> permissoes
) {
}
