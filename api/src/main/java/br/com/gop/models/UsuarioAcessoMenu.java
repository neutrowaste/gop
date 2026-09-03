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

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "usuario_acesso_menu", uniqueConstraints = {
	@UniqueConstraint(name = "uk_usuario_acesso_menu", columnNames = {"usuario_id", "codigo"})
})
public class UsuarioAcessoMenu extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(nullable = false, length = 50)
	private String codigo;

	@Column(nullable = false)
	private boolean liberado = false;

	@Column(name = "desde")
	private Instant desde;
}
