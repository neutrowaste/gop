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
@Table(name = "papel_permissao", uniqueConstraints = {
	@UniqueConstraint(name = "uk_papel_permissao", columnNames = {"papel_id", "permissao_id"})
})
public class PapelPermissao extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "papel_id", nullable = false)
	private Papel papel;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "permissao_id", nullable = false)
	private Permissao permissao;
}
