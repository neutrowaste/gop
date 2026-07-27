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
@Table(name = "permissao", uniqueConstraints = {
	@UniqueConstraint(name = "uk_permissao_codigo", columnNames = "codigo")
})
public class Permissao extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String codigo;

	@Column(nullable = false, length = 200)
	private String descricao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "modulo_id")
	private Modulo modulo;

	@Column(nullable = false)
	private boolean ativo = true;
}
