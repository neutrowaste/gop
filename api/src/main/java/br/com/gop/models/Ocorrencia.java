package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro automático de desvio detectado pelo motor de Varrição (RN-001):
 * ordem de execução do plano pulada ou GPS em silêncio. Auditável via
 * resolução/justificativa (mesma trilha de AuditoriaService dos demais módulos).
 */
@Getter
@Setter
@Entity
@Table(name = "ocorrencia")
public class Ocorrencia extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "escala_id", nullable = false)
	private Escala escala;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipamento_id", nullable = false)
	private Equipamento equipamento;

	/** Nulo para GPS_SILENCIOSO (ocorrência em nível de equipamento, não de trecho). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trecho_execucao_id")
	private TrechoExecucao trechoExecucao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TipoOcorrencia tipo;

	@Column(nullable = false, length = 500)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusOcorrencia status = StatusOcorrencia.ABERTA;

	@Column(name = "detectada_em", nullable = false)
	private Instant detectadaEm;

	@Column(name = "resolvida_em")
	private Instant resolvidaEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolvida_por_usuario_id")
	private Usuario resolvidaPorUsuario;

	@Column(name = "observacao_resolucao", length = 500)
	private String observacaoResolucao;

	@Column(nullable = false)
	private boolean ativo = true;
}
