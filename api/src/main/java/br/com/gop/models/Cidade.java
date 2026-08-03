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

@Getter
@Setter
@Entity
@Table(name = "cidade", uniqueConstraints = {
	@UniqueConstraint(name = "uk_cidade_empresa_nome_uf", columnNames = {"empresa_id", "nome", "uf"})
})
public class Cidade extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, length = 2)
	private String uf = "RJ";

	@Column(nullable = false)
	private boolean ativo = true;
}
