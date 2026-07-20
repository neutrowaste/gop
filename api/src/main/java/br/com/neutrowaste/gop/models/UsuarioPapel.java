package br.com.neutrowaste.gop.models;

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
@Table(name = "usuario_papel", uniqueConstraints = {
	@UniqueConstraint(name = "uk_usuario_papel_empresa", columnNames = {"usuario_id", "papel_id", "empresa_id"})
})
public class UsuarioPapel extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "papel_id", nullable = false)
	private Papel papel;

	/** Escopo organizacional do papel; nulo para papéis de plataforma. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id")
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "filial_id")
	private Filial filial;
}
