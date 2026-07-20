package br.com.neutrowaste.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "modulo", uniqueConstraints = {
	@UniqueConstraint(name = "uk_modulo_codigo", columnNames = "codigo")
})
public class Modulo extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String codigo;

	@Column(nullable = false, length = 120)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CategoriaModulo categoria;

	@Column(nullable = false)
	private int ordem = 0;

	@Column(nullable = false)
	private boolean ativo = true;
}
