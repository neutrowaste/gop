package br.com.gop.models;

import java.util.List;

public record RasSystemSyncResponse(
	int lidos,
	int ingeridos,
	int ignorados,
	int naoVinculados,
	List<RasSystemSyncItemResponse> itens
) {
}
