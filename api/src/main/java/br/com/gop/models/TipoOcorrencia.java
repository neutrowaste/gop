package br.com.gop.models;

/** Tipos de ocorrência gerados automaticamente pelo motor de Varrição (RN-001). */
public enum TipoOcorrencia {
	/** Equipe concluiu um trecho fora da ordem planejada (pulou trecho anterior/posterior pendente). */
	ORDEM_PLANO_VIOLADA,
	/** Ativo GPS ficou em silêncio de transmissão além do limiar configurado. */
	GPS_SILENCIOSO
}
