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
@Table(name = "usuario_modulo", uniqueConstraints = {
	@UniqueConstraint(name = "uk_usuario_modulo", columnNames = {"usuario_id", "modulo_id"})
})
public class UsuarioModulo extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "modulo_id", nullable = false)
	private Modulo modulo;

	@Column(nullable = false)
	private boolean liberado = false;

	@Column(name = "desde")
	private Instant desde;
}
