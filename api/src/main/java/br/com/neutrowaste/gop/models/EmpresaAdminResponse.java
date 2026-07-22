package br.com.neutrowaste.gop.models;

import java.time.Instant;

public record EmpresaAdminResponse(
	Long id,
	Long empresaId,
	String email,
	String nome,
	boolean ativo,
	Instant criadoEm,
	Instant atualizadoEm
) {
}
