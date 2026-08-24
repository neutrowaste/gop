package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Equipamento operacional (Lutocar / GPS lógico).
 * Consome o teto de ativos da licença vigente enquanto estiver ativo.
 */
@Getter
@Setter
@Entity
@Table(name = "equipamento", uniqueConstraints = {
	@UniqueConstraint(name = "uk_equipamento_empresa_codigo", columnNames = {"empresa_id", "codigo"}),
	@UniqueConstraint(name = "uk_equipamento_empresa_codigo_externo", columnNames = {"empresa_id", "codigo_externo"})
})
public class Equipamento extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(nullable = false, length = 60)
	private String codigo;

	@Column(length = 200)
	private String descricao;

	/** idVeiculo (RasSystem) ou serial do GPS próprio. */
	@Column(name = "codigo_externo", length = 80)
	private String codigoExterno;

	@Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(name = "fonte", length = 20)
	private FonteGps fonte;

	@Column(nullable = false)
	private boolean ativo = true;
}
