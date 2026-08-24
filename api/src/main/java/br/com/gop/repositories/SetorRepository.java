package br.com.gop.repositories;

import br.com.gop.models.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SetorRepository extends JpaRepository<Setor, Long> {

	@Query("""
		select s
		from Setor s
		join fetch s.modulo m
		join fetch s.cidade
		where s.empresa.id = :empresaId
		  and (:cidadeId is null or s.cidade.id = :cidadeId)
		  and (:moduloId is null or m.id = :moduloId)
		order by m.ordem asc, s.nome asc
		""")
	List<Setor> findByEmpresaFiltrado(
		@Param("empresaId") Long empresaId,
		@Param("cidadeId") Long cidadeId,
		@Param("moduloId") Long moduloId
	);

	@Query("""
		select s
		from Setor s
		join fetch s.modulo
		join fetch s.cidade
		where s.id = :id and s.empresa.id = :empresaId
		""")
	Optional<Setor> findByIdAndEmpresaId(@Param("id") Long id, @Param("empresaId") Long empresaId);

	boolean existsByEmpresaIdAndModuloIdAndCodigoIgnoreCase(Long empresaId, Long moduloId, String codigo);

	boolean existsByEmpresaIdAndModuloIdAndCodigoIgnoreCaseAndIdNot(
		Long empresaId,
		Long moduloId,
		String codigo,
		Long id
	);
}
