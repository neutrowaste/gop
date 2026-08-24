package br.com.gop.models;

import java.util.List;

public record RasSystemImportarRastreioResponse(
	Long idVeiculo,
	String veiculo,
	int rastreio,
	int eventos,
	int ingeridos,
	int ignorados,
	boolean vinculado,
	Long equipamentoId,
	List<TelemetriaGps> pontos,
	List<TelemetriaGps> eventosLidos
) {
}
