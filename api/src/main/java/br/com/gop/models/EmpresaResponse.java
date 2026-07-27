package br.com.gop.models;

import java.time.Instant;

public record EmpresaResponse(
	Long id,
	String codigo,
	String nome,
	boolean ativo,
	Instant criadoEm,
	Instant atualizadoEm
) {
}
