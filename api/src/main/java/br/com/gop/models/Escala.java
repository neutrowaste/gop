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
import java.time.LocalDate;

/**
 * Amarração diária obrigatória do motor de Varrição (RN-001):
 * Setor ↔ Equipe ↔ Ativo GPS (Equipamento/Lutocar) ↔ Supervisor ↔ Turno.
 * Uma linha = uma amarração completa; múltiplos lutocares da mesma equipe no
 * mesmo turno geram múltiplas linhas de Escala.
 */
@Getter
@Setter
@Entity
@Table(name = "escala", uniqueConstraints = {
	@UniqueConstraint(
		name = "uk_escala_empresa_data_turno_equipamento",
		columnNames = {"empresa_id", "data", "turno", "equipamento_id"}
	)
})
public class Escala extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(nullable = false)
	private LocalDate data;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Turno turno;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "setor_id", nullable = false)
	private Setor setor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipe_id", nullable = false)
	private Equipe equipe;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipamento_id", nullable = false)
	private Equipamento equipamento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "supervisor_usuario_id", nullable = false)
	private Usuario supervisor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusEscala status = StatusEscala.RASCUNHO;

	@Column(name = "publicada_em")
	private Instant publicadaEm;

	/** false = cancelada (nunca excluir registro, preserva histórico da amarração do dia). */
	@Column(nullable = false)
	private boolean ativo = true;
}
