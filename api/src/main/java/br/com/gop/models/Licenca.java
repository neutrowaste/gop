package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "licenca")
public class Licenca extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(name = "teto_ativos", nullable = false)
	private int tetoAtivos;

	@Column(name = "vigencia_inicio", nullable = false)
	private LocalDate vigenciaInicio;

	@Column(name = "vigencia_fim")
	private LocalDate vigenciaFim;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusLicenca status = StatusLicenca.ATIVA;

	@Column(nullable = false)
	private boolean ativo = true;
}
