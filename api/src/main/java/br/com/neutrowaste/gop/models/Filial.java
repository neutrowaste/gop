package br.com.neutrowaste.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "filial", uniqueConstraints = {
	@UniqueConstraint(name = "uk_filial_empresa_codigo", columnNames = {"empresa_id", "codigo_externo"})
})
public class Filial extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	/** Alinha ao idFilial da API RasSystem. */
	@Column(name = "codigo_externo", nullable = false, length = 80)
	private String codigoExterno;

	@Column(nullable = false, length = 200)
	private String nome;

	@Column(nullable = false)
	private boolean ativo = true;
}
