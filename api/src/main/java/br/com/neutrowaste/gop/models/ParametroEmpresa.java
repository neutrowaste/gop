package br.com.neutrowaste.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Parâmetros operacionais configuráveis por empresa (não hardcode no motor).
 * Exemplos de chave: cobertura_minima_pct, silencio_gps_minutos, tolerancia_sinal_minutos.
 */
@Getter
@Setter
@Entity
@Table(name = "parametro_empresa", uniqueConstraints = {
	@UniqueConstraint(name = "uk_parametro_empresa_chave", columnNames = {"empresa_id", "chave"})
})
public class ParametroEmpresa extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(nullable = false, length = 80)
	private String chave;

	@Column(nullable = false, length = 500)
	private String valor;

	@Column(length = 30)
	private String tipo = "STRING";

	@Column(length = 255)
	private String descricao;

	@Column(nullable = false)
	private boolean ativo = true;
}
