package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa", uniqueConstraints = {
	@UniqueConstraint(name = "uk_empresa_codigo", columnNames = "codigo")
})
public class Empresa extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String codigo;

	@Column(nullable = false, length = 200)
	private String nome;

	@Column(nullable = false)
	private boolean ativo = true;
}
