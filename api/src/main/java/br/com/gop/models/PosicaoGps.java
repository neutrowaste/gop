package br.com.gop.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

/**
 * Ingestão bruta de posição GPS (RN-001). Fica salva independente de haver
 * Escala PUBLICADA correspondente; o motor de cobertura só processa a
 * posição (snap + regras) quando encontra a amarração do dia/turno.
 *
 * TODO(fase 3+): particionar por data/tenant conforme volume real (RN do
 * .cursorrules); "ddl-auto: update" do Hibernate não gerencia partições
 * nativas do PostgreSQL, então este passo fica para uma migration dedicada.
 */
@Getter
@Setter
@Entity
@Table(name = "posicao_gps", indexes = {
	@Index(name = "ix_posicao_gps_empresa_equipamento_capturado", columnList = "empresa_id, equipamento_id, capturado_em")
})
public class PosicaoGps extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipamento_id", nullable = false)
	private Equipamento equipamento;

	@JdbcTypeCode(SqlTypes.GEOMETRY)
	@Column(nullable = false, columnDefinition = "geometry(Point,4326)")
	private Point geom;

	@Column(name = "velocidade_kmh")
	private Float velocidadeKmh;

	@Column(name = "precisao_metros")
	private Float precisaoMetros;

	/** Instante em que o dispositivo capturou a posição (informado no payload). */
	@Column(name = "capturado_em", nullable = false)
	private Instant capturadoEm;

	/** Instante em que a API recebeu/persistiu a posição. */
	@Column(name = "recebido_em", nullable = false)
	private Instant recebidoEm;

	/** Trecho do plano do dia em que a posição foi "encaixada" pelo snap PostGIS (nulo = fora do plano/sem escala). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trecho_execucao_id")
	private TrechoExecucao trechoExecucao;

	@Column(name = "distancia_eixo_metros")
	private Float distanciaEixoMetros;

	/** ST_LineLocatePoint do trecho encontrado (0.0 início — 1.0 fim). */
	@Column(name = "progresso_linear")
	private Float progressoLinear;

	/** true = dentro do buffer de snap configurado (posição "válida" para RN-001). */
	@Column(nullable = false)
	private boolean valido = false;
}
