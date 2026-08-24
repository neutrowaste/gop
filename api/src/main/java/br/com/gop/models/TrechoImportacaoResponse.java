package br.com.gop.models;

import java.util.List;

public record TrechoImportacaoResponse(
	int lidos,
	int criados,
	int ignorados,
	List<TrechoImportacaoItemResponse> itens
) {
}
