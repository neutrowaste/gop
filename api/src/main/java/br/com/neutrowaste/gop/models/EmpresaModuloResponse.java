package br.com.neutrowaste.gop.models;

import java.time.Instant;

public record EmpresaModuloResponse(
	Long moduloId,
	String codigo,
	String nome,
	CategoriaModulo categoria,
	int ordem,
	boolean habilitado,
	Instant desde
) {
}
