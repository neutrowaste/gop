package br.com.gop.models;

import java.util.List;

public record RasSystemStatusResponse(
	boolean configurado,
	String baseUrl,
	String login,
	Integer idFilial,
	Integer idRotulo,
	boolean pingOk,
	int filiais,
	String erro
) {
}
