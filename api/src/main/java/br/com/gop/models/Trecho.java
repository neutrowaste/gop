package br.com.gop.models;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.LineString;

import java.time.Instant;

/**
 * Segmento auditável do Setor. O nome da via ({@code nomeRua}) vem do
 * shapefile (atributo RUA) ou do cadastro manual — não há entidade Rua.
 */
@Getter
@Setter
@Entity
@Table(name = "trecho", uniqueConstraints = {
	@UniqueConstraint(name = "uk_trecho_empresa_codigo", columnNames = {"empresa_id", "codigo"})
})
public class Trecho extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "setor_id", nullable = false)
	private Setor setor;

	/** Nome do logradouro (shapefile RUA / cadastro manual). */
	@Column(name = "nome_rua", length = 200)
	private String nomeRua;

	@Column(nullable = false, length = 80)
	private String codigo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusTrecho status = StatusTrecho.PLANEJADO;

	@Column(name = "percentual_executado", nullable = false)
	private float percentualExecutado = 0f;

	@Column(name = "comprimento_metros", nullable = false)
	private float comprimentoMetros = 0f;

	@Column(name = "ultima_visita_em")
	private Instant ultimaVisitaEm;

	@Column(name = "em_correcao", nullable = false)
	private boolean emCorrecao = false;

	@Column(name = "liberado_informe", nullable = false)
	private boolean liberadoInforme = false;

	@JdbcTypeCode(SqlTypes.GEOMETRY)
	@Column(nullable = false, columnDefinition = "geometry(LineString,4326)")
	private LineString geom;

	@Column(nullable = false)
	private boolean ativo = true;
}
