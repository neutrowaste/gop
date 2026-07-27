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

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ativo_historico")
public class AtivoHistorico extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ativo_id", nullable = false)
	private Ativo ativo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_anterior", length = 20)
	private StatusAtivo statusAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_novo", nullable = false, length = 20)
	private StatusAtivo statusNovo;

	@Column(length = 500)
	private String motivo;

	@Column(name = "ocorrido_em", nullable = false)
	private Instant ocorridoEm;
}
