package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Equipamento operacional da equipe (Lutocar / GPS lógico).
 * Consome o teto de ativos da licença vigente enquanto estiver ativo.
 */
@Getter
@Setter
@Entity
@Table(name = "equipamento", uniqueConstraints = {
	@UniqueConstraint(name = "uk_equipamento_empresa_codigo", columnNames = {"empresa_id", "codigo"})
})
public class Equipamento extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipe_id", nullable = false)
	private Equipe equipe;

	@Column(nullable = false, length = 60)
	private String codigo;

	@Column(length = 200)
	private String descricao;

	@Column(nullable = false)
	private boolean ativo = true;
}
