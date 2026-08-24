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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.MultiPolygon;

@Getter
@Setter
@Entity
@Table(name = "setor", uniqueConstraints = {
	@UniqueConstraint(name = "uk_setor_empresa_modulo_codigo", columnNames = {"empresa_id", "modulo_id", "codigo"})
})
public class Setor extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "modulo_id", nullable = false)
	private Modulo modulo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cidade_id", nullable = false)
	private Cidade cidade;

	/** Código operacional (ex.: DFA001), espelho de Sector.code no legado. */
	@Column(length = 40)
	private String codigo;

	@Column(nullable = false, length = 200)
	private String nome;

	/** Polígono opcional de demarcação (shapefile de área). Ausente quando o setor opera por trechos. */
	@JdbcTypeCode(SqlTypes.GEOMETRY)
	@Column(columnDefinition = "geometry(MultiPolygon,4326)")
	private MultiPolygon geom;

	@Column(nullable = false)
	private boolean ativo = true;
}
