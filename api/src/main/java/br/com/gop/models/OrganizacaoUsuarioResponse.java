package br.com.gop.models;

import java.time.Instant;
import java.util.List;

public record OrganizacaoUsuarioResponse(
	Long id,
	Long empresaId,
	String email,
	String nome,
	boolean ativo,
	boolean administradorEmpresa,
	List<String> papeis,
	Instant criadoEm,
	Instant atualizadoEm
) {
}
