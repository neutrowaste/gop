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
 * Cadastro geo + campos de pré-auditoria (paridade Segment do legado).
 * Associação a escala/setor do dia entra na Fase 2 (Route).
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
	@JoinColumn(name = "rua_id", nullable = false)
	private Rua rua;

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
