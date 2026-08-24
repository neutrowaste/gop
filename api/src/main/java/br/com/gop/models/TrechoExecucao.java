package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Plano diário do motor de Varrição (RN-001): representa "este Trecho está no
 * plano desta Escala, nesta ordem, com este status/percentual do dia".
 * O cadastro de {@link Trecho} continua existindo só como geometria/código;
 * o estado operacional diário passa a viver aqui.
 */
@Getter
@Setter
@Entity
@Table(name = "trecho_execucao", uniqueConstraints = {
	@UniqueConstraint(name = "uk_trecho_execucao_escala_trecho", columnNames = {"escala_id", "trecho_id"}),
	@UniqueConstraint(name = "uk_trecho_execucao_escala_ordem", columnNames = {"escala_id", "ordem"})
})
public class TrechoExecucao extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "escala_id", nullable = false)
	private Escala escala;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trecho_id", nullable = false)
	private Trecho trecho;

	/** Sequência planejada de execução dentro da Escala (1, 2, 3...). */
	@Column(nullable = false)
	private int ordem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusTrecho status = StatusTrecho.PLANEJADO;

	/** Maior progresso linear (ST_LineLocatePoint) já observado, em percentual (0-100). */
	@Column(name = "percentual_executado", nullable = false)
	private float percentualExecutado;

	/** Total de posições GPS válidas (dentro do buffer) já processadas para este trecho. */
	@Column(name = "quantidade_posicoes_validas", nullable = false)
	private int quantidadePosicoesValidas;

	/** RN-001: EXECUTADO exige pelo menos 1 posição válida em cada faixa (início/meio/fim). */
	@Column(name = "teve_posicao_inicio", nullable = false)
	private boolean tevePosicaoInicio;

	@Column(name = "teve_posicao_meio", nullable = false)
	private boolean tevePosicaoMeio;

	@Column(name = "teve_posicao_fim", nullable = false)
	private boolean tevePosicaoFim;

	@Column(name = "primeira_posicao_valida_em")
	private Instant primeiraPosicaoValidaEm;

	@Column(name = "ultima_posicao_valida_em")
	private Instant ultimaPosicaoValidaEm;

	/** false = removido do plano (editável na avulsa e no modelo de frequência). */
	@Column(nullable = false)
	private boolean ativo = true;
}
