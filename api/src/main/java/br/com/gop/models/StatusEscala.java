package br.com.gop.models;

/**
 * Ciclo de vida da Escala diária.
 * O motor de regras da Varrição só processa posições de escalas PUBLICADA (RN-001).
 */
public enum StatusEscala {
	RASCUNHO,
	PUBLICADA
}
