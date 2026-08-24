package br.com.gop.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Amarração do motor de Varrição (RN-001):
 * Setor ↔ Colaboradores (cada um já designado a um Lutocar) ↔ Supervisor ↔ Turno.
 * Modelos de frequência (data nula) geram uma instância PUBLICADA por dia correspondente.
 */
@Getter
@Setter
@Entity
@Table(name = "escala")
public class Escala extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column
	private LocalDate data;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Turno turno;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoEscala tipo = TipoEscala.AVULSA;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "origem_id")
	private Escala origem;

	@ElementCollection
	@CollectionTable(name = "escala_dia_semana", joinColumns = @JoinColumn(name = "escala_id", nullable = false))
	@Enumerated(EnumType.STRING)
	@Column(name = "dia_semana", nullable = false, length = 10)
	private Set<DayOfWeek> diasSemana = new LinkedHashSet<>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "setor_id", nullable = false)
	private Setor setor;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "escala_colaborador",
		joinColumns = @JoinColumn(name = "escala_id", nullable = false),
		inverseJoinColumns = @JoinColumn(name = "colaborador_id", nullable = false)
	)
	private Set<Colaborador> colaboradores = new LinkedHashSet<>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "supervisor_usuario_id", nullable = false)
	private Usuario supervisor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusEscala status = StatusEscala.RASCUNHO;

	@Column(name = "publicada_em")
	private Instant publicadaEm;

	/** false = cancelada (nunca excluir registro, preserva histórico da amarração do dia). */
	@Column(nullable = false)
	private boolean ativo = true;

	public boolean isModeloFrequencia() {
		return tipo == TipoEscala.FREQUENCIA && origem == null;
	}

	public boolean isInstanciaFrequencia() {
		return tipo == TipoEscala.FREQUENCIA && origem != null;
	}

	public Set<Equipamento> equipamentosDosColaboradores() {
		return colaboradores.stream()
			.map(Colaborador::getEquipamento)
			.filter(equipamento -> equipamento != null)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public String nomesColaboradores() {
		return colaboradores.stream()
			.map(Colaborador::getNome)
			.sorted()
			.collect(Collectors.joining(", "));
	}
}
