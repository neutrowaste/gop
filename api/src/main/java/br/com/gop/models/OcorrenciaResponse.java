package br.com.gop.models;

import java.time.Instant;
import java.time.LocalDate;

public record OcorrenciaResponse(
	Long id,
	Long escalaId,
	LocalDate escalaData,
	String setorNome,
	String colaboradoresNomes,
	Long equipamentoId,
	String equipamentoCodigo,
	Long trechoExecucaoId,
	String trechoCodigo,
	TipoOcorrencia tipo,
	String descricao,
	StatusOcorrencia status,
	Instant detectadaEm,
	Instant resolvidaEm,
	String resolvidaPorNome,
	String observacaoResolucao,
	boolean ativo
) {
}
