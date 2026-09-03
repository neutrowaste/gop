package br.com.gop.models;

import java.time.Instant;

public record UsuarioAcessoMenuResponse(
	String codigo,
	String nome,
	String grupo,
	int ordem,
	boolean liberado,
	Instant desde
) {
	public static UsuarioAcessoMenuResponse de(AcessoMenu item, boolean liberado, Instant desde) {
		return new UsuarioAcessoMenuResponse(
			item.name(),
			item.nome(),
			item.grupo(),
			item.ordem(),
			liberado,
			desde
		);
	}
}
