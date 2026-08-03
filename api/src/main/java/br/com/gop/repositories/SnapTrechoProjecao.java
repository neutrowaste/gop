package br.com.gop.repositories;

/** Resultado do snap PostGIS de uma posição GPS no plano do dia (trecho_execucao). */
public interface SnapTrechoProjecao {

	Long getId();

	Double getDistanciaMetros();

	Double getProgressoLinear();
}
