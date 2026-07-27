package br.com.gop.models;

import java.time.Instant;

public record UsuarioModuloResponse(
	Long moduloId,
	String codigo,
	String nome,
	CategoriaModulo categoria,
	int ordem,
	boolean contratado,
	boolean liberado,
	Instant desde
) {
}
