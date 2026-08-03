package br.com.gop.models;

/**
 * Situação de sinal do ativo GPS para exibição no mapa operacional
 * (RN-001: silêncio >= 60min é falha, com tolerância de 30min de perda de sinal).
 */
public enum SituacaoSinal {
	/** Última posição dentro da tolerância de sinal. */
	ONLINE,
	/** Já passou da tolerância, mas ainda não atingiu o limiar de silêncio (falha). */
	ATENCAO,
	/** Atingiu ou ultrapassou o limiar de silêncio do GPS. */
	OFFLINE
}
