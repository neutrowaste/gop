package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.LineString;

@Getter
@Setter
@Entity
@Table(name = "rua")
public class Rua extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cidade_id", nullable = false)
	private Cidade cidade;

	@Column(nullable = false, length = 200)
	private String nome;

	/** Eixo viário LineString WGS84 — paridade com Street.geom do legado. */
	@JdbcTypeCode(SqlTypes.GEOMETRY)
	@Column(nullable = false, columnDefinition = "geometry(LineString,4326)")
	private LineString geom;

	@Column(nullable = false)
	private boolean ativo = true;
}
