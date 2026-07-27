package br.com.gop.models;

import java.util.List;

public record UsuarioSessaoResponse(
	Long usuarioId,
	Long empresaId,
	String empresaNome,
	String empresaCodigo,
	String nome,
	String email,
	List<String> papeis,
	List<String> permissoes
) {
}
