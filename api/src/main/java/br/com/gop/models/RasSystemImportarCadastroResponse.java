package br.com.gop.models;

import java.util.List;

public record RasSystemImportarCadastroResponse(
	int solicitados,
	int criados,
	int jaExistentes,
	int ignorados,
	List<RasSystemImportarCadastroItemResponse> itens,
	RasSystemSyncResponse sync
) {
}
