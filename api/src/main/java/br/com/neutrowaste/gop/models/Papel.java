package br.com.neutrowaste.gop.models;

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

@Getter
@Setter
@Entity
@Table(name = "papel", uniqueConstraints = {
	@UniqueConstraint(name = "uk_papel_empresa_codigo", columnNames = {"empresa_id", "codigo"})
})
public class Papel extends BaseEntity {

	/** Nulo para papéis de sistema (globais); preenchido para papéis customizados do tenant. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id")
	private Empresa empresa;

	@Column(nullable = false, length = 50)
	private String codigo;

	@Column(nullable = false, length = 120)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EscopoPapel escopo;

	@Column(nullable = false)
	private boolean ativo = true;
}
