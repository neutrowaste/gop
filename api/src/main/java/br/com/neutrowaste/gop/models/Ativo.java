package br.com.neutrowaste.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "ativo", uniqueConstraints = {
	@UniqueConstraint(name = "uk_ativo_empresa_lut", columnNames = {"empresa_id", "codigo_lut"})
})
public class Ativo extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "filial_id")
	private Filial filial;

	/** Identificador lógico do GPS (ex.: LUT-0000). */
	@Column(name = "codigo_lut", nullable = false, length = 40)
	private String codigoLut;

	@Column(length = 120)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusAtivo status = StatusAtivo.ATIVO;

	/** false = desativado (nunca excluir registro). */
	@Column(nullable = false)
	private boolean ativo = true;
}
