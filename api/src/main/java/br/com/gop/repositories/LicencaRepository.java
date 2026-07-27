package br.com.gop.repositories;

import br.com.gop.models.Licenca;
import br.com.gop.models.StatusLicenca;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LicencaRepository extends JpaRepository<Licenca, Long> {

	@EntityGraph(attributePaths = "empresa")
	List<Licenca> findByEmpresaIdOrderByVigenciaInicioDesc(Long empresaId);

	@EntityGraph(attributePaths = "empresa")
	@Query("""
		select l
		from Licenca l
		where l.id = :id
		""")
	Optional<Licenca> findComEmpresaById(@Param("id") Long id);

	@EntityGraph(attributePaths = "empresa")
	@Query("""
		select l
		from Licenca l
		where l.empresa.id = :empresaId
		  and l.ativo = true
		  and l.status = :status
		  and l.vigenciaInicio <= :data
		  and (l.vigenciaFim is null or l.vigenciaFim >= :data)
		""")
	Optional<Licenca> findVigente(
		@Param("empresaId") Long empresaId,
		@Param("status") StatusLicenca status,
		@Param("data") LocalDate data
	);

	@Query("""
		select count(l) > 0
		from Licenca l
		where l.empresa.id = :empresaId
		  and l.ativo = true
		  and l.status <> :encerrada
		  and (:ignorarId is null or l.id <> :ignorarId)
		  and (:fim is null or l.vigenciaInicio <= :fim)
		  and (l.vigenciaFim is null or l.vigenciaFim >= :inicio)
		""")
	boolean existeSobreposicao(
		@Param("empresaId") Long empresaId,
		@Param("inicio") LocalDate inicio,
		@Param("fim") LocalDate fim,
		@Param("ignorarId") Long ignorarId,
		@Param("encerrada") StatusLicenca encerrada
	);
}
