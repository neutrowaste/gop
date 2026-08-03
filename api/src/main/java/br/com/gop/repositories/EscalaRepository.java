package br.com.gop.repositories;

import br.com.gop.models.Escala;
import br.com.gop.models.StatusEscala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EscalaRepository extends JpaRepository<Escala, Long>, JpaSpecificationExecutor<Escala> {

	Optional<Escala> findByIdAndEmpresaId(Long id, Long empresaId);

	boolean existsByEmpresaIdAndDataAndTurnoAndEquipamentoId(
		Long empresaId,
		LocalDate data,
		br.com.gop.models.Turno turno,
		Long equipamentoId
	);

	boolean existsByEmpresaIdAndDataAndTurnoAndEquipamentoIdAndIdNot(
		Long empresaId,
		LocalDate data,
		br.com.gop.models.Turno turno,
		Long equipamentoId,
		Long id
	);

	/** Usado pelo motor de cobertura para resolver a amarração do dia de um ativo GPS. */
	List<Escala> findByEmpresaIdAndEquipamentoIdAndDataAndStatusAndAtivoTrue(
		Long empresaId,
		Long equipamentoId,
		LocalDate data,
		StatusEscala status
	);

	/** Usado pelo SaudeGpsScheduler para varrer todas as escalas publicadas do dia (todas as empresas). */
	List<Escala> findByStatusAndDataAndAtivoTrue(StatusEscala status, LocalDate data);

	/** Usado pelo mapa operacional para montar o retrato do dia de uma única empresa. */
	List<Escala> findByEmpresaIdAndDataAndStatusAndAtivoTrue(Long empresaId, LocalDate data, StatusEscala status);
}
