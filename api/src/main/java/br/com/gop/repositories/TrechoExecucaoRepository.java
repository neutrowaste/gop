package br.com.gop.repositories;

import br.com.gop.models.TrechoExecucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrechoExecucaoRepository extends JpaRepository<TrechoExecucao, Long> {

	List<TrechoExecucao> findByEscalaIdAndAtivoTrueOrderByOrdemAsc(Long escalaId);

	Optional<TrechoExecucao> findByIdAndEscalaIdAndEmpresaId(Long id, Long escalaId, Long empresaId);

	boolean existsByEscalaIdAndTrechoIdAndAtivoTrue(Long escalaId, Long trechoId);

	boolean existsByEscalaIdAndOrdemAndAtivoTrue(Long escalaId, int ordem);

	boolean existsByEscalaIdAndOrdemAndAtivoTrueAndIdNot(Long escalaId, int ordem, Long id);

	List<TrechoExecucao> findByEscalaIdAndOrdemLessThanAndAtivoTrue(Long escalaId, int ordem);

	List<TrechoExecucao> findByEscalaIdAndOrdemGreaterThanAndAtivoTrue(Long escalaId, int ordem);

	/**
	 * Snap geoespacial (padrão técnico do legado apimonitoragps): acha, dentro do
	 * plano do dia (trecho_execucao da Escala), o trecho mais próximo do ponto
	 * GPS informado, respeitando o buffer configurado (ST_DWithin) e calculando
	 * o progresso linear ao longo do eixo (ST_LineLocatePoint).
	 */
	@Query(
		value = """
			select te.id as id,
			       st_distance(
			           t.geom::geography,
			           st_setsrid(st_makepoint(:longitude, :latitude), 4326)::geography
			       ) as distanciaMetros,
			       st_linelocatepoint(
			           t.geom,
			           st_setsrid(st_makepoint(:longitude, :latitude), 4326)
			       ) as progressoLinear
			from trecho_execucao te
			join trecho t on t.id = te.trecho_id
			where te.escala_id = :escalaId
			  and te.ativo = true
			  and st_dwithin(
			      t.geom::geography,
			      st_setsrid(st_makepoint(:longitude, :latitude), 4326)::geography,
			      :bufferMetros
			  )
			order by t.geom <-> st_setsrid(st_makepoint(:longitude, :latitude), 4326)
			limit 1
			""",
		nativeQuery = true
	)
	Optional<SnapTrechoProjecao> encontrarTrechoMaisProximo(
		@Param("escalaId") Long escalaId,
		@Param("longitude") double longitude,
		@Param("latitude") double latitude,
		@Param("bufferMetros") double bufferMetros
	);
}
