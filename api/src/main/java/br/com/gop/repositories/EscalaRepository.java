package br.com.gop.repositories;

import br.com.gop.models.Escala;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.TipoEscala;
import br.com.gop.models.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EscalaRepository extends JpaRepository<Escala, Long>, JpaSpecificationExecutor<Escala> {

	Optional<Escala> findByIdAndEmpresaId(Long id, Long empresaId);

	/** Usado pelo motor de cobertura para resolver a amarração do dia de um ativo GPS. */
	@Query("""
		SELECT DISTINCT e FROM Escala e
		JOIN e.colaboradores c
		WHERE e.empresa.id = :empresaId
			AND c.equipamento.id = :equipamentoId
			AND e.data = :data
			AND e.status = :status
			AND e.ativo = true
		""")
	List<Escala> findPublicadasDoAtivoNoDia(
		@Param("empresaId") Long empresaId,
		@Param("equipamentoId") Long equipamentoId,
		@Param("data") LocalDate data,
		@Param("status") StatusEscala status
	);

	@Query("""
		SELECT DISTINCT e FROM Escala e
		JOIN e.colaboradores c
		WHERE e.empresa.id = :empresaId
			AND e.data = :data
			AND e.turno = :turno
			AND e.ativo = true
			AND c.id IN :colaboradorIds
		""")
	List<Escala> findAtivasComColaboradoresNoTurno(
		@Param("empresaId") Long empresaId,
		@Param("data") LocalDate data,
		@Param("turno") Turno turno,
		@Param("colaboradorIds") Collection<Long> colaboradorIds
	);

	@Query("""
		SELECT DISTINCT e FROM Escala e
		JOIN e.colaboradores c
		WHERE e.empresa.id = :empresaId
			AND e.data = :data
			AND e.turno = :turno
			AND e.ativo = true
			AND c.id IN :colaboradorIds
			AND e.id <> :idAtual
		""")
	List<Escala> findAtivasComColaboradoresNoTurnoExcluindo(
		@Param("empresaId") Long empresaId,
		@Param("data") LocalDate data,
		@Param("turno") Turno turno,
		@Param("colaboradorIds") Collection<Long> colaboradorIds,
		@Param("idAtual") Long idAtual
	);

	@Query("""
		SELECT DISTINCT e FROM Escala e
		JOIN e.colaboradores c
		WHERE e.empresa.id = :empresaId
			AND e.data = :data
			AND e.turno = :turno
			AND e.ativo = true
			AND c.equipamento.id IN :equipamentoIds
		""")
	List<Escala> findAtivasComEquipamentosNoTurno(
		@Param("empresaId") Long empresaId,
		@Param("data") LocalDate data,
		@Param("turno") Turno turno,
		@Param("equipamentoIds") Collection<Long> equipamentoIds
	);

	@Query("""
		SELECT DISTINCT e FROM Escala e
		JOIN e.colaboradores c
		WHERE e.empresa.id = :empresaId
			AND e.data = :data
			AND e.turno = :turno
			AND e.ativo = true
			AND c.equipamento.id IN :equipamentoIds
			AND e.id <> :idAtual
		""")
	List<Escala> findAtivasComEquipamentosNoTurnoExcluindo(
		@Param("empresaId") Long empresaId,
		@Param("data") LocalDate data,
		@Param("turno") Turno turno,
		@Param("equipamentoIds") Collection<Long> equipamentoIds,
		@Param("idAtual") Long idAtual
	);

	/** Usado pelo SaudeGpsScheduler para varrer todas as escalas publicadas do dia (todas as empresas). */
	List<Escala> findByStatusAndDataAndAtivoTrue(StatusEscala status, LocalDate data);

	/** Usado pelo mapa operacional para montar o retrato do dia de uma única empresa. */
	List<Escala> findByEmpresaIdAndDataAndStatusAndAtivoTrue(Long empresaId, LocalDate data, StatusEscala status);

	List<Escala> findByTipoAndOrigemIsNullAndAtivoTrue(TipoEscala tipo);

	List<Escala> findByEmpresaIdAndTipoAndOrigemIsNullAndAtivoTrue(Long empresaId, TipoEscala tipo);

	boolean existsByOrigemIdAndData(Long origemId, LocalDate data);

	Optional<Escala> findByOrigemIdAndDataAndAtivoTrue(Long origemId, LocalDate data);

	List<Escala> findByOrigemIdAndAtivoTrue(Long origemId);
}
