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

/** Mão de obra (paridade Gari do legado); nomenclatura genérica no núcleo. */
@Getter
@Setter
@Entity
@Table(name = "colaborador", uniqueConstraints = {
	@UniqueConstraint(name = "uk_colaborador_empresa_matricula", columnNames = {"empresa_id", "matricula"})
})
public class Colaborador extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipe_id", nullable = false)
	private Equipe equipe;

	@Column(nullable = false, length = 200)
	private String nome;

	@Column(nullable = false, length = 60)
	private String matricula;

	@Column(nullable = false)
	private boolean ativo = true;
}
