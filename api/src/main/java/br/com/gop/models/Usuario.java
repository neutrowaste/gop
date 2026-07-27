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
@Table(name = "usuario", uniqueConstraints = {
	@UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
})
public class Usuario extends BaseEntity {

	/** Nulo apenas para admin de plataforma (GOP). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id")
	private Empresa empresa;

	@Column(nullable = false, length = 200)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	@Column(nullable = false, length = 200)
	private String nome;

	@Column(nullable = false)
	private boolean ativo = true;
}
