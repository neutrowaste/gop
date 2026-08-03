package br.com.gop.models;

/**
 * Paridade com SegmentStatus do legado NeutroWaste (pré-auditoria).
 * Estado operacional no próprio trecho até existir trecho_execucao (fases 2–4).
 */
public enum StatusTrecho {
	PLANEJADO,
	EXECUTADO,
	SUSPEITO,
	PENDENTE,
	JUSTIFICADO,
	FORA_DA_ROTA
}
