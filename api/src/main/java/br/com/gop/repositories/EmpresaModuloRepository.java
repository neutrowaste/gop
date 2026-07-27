package br.com.gop.repositories;

import br.com.gop.models.EmpresaModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmpresaModuloRepository extends JpaRepository<EmpresaModulo, Long> {

	@Query("""
		select em.modulo.id
		from EmpresaModulo em
		where em.empresa.id = :empresaId
		  and em.habilitado = true
		  and em.modulo.ativo = true
		""")
	Set<Long> findModuloIdsHabilitadosByEmpresaId(@Param("empresaId") Long empresaId);

	@Query("""
		select em
		from EmpresaModulo em
		join fetch em.modulo
		where em.empresa.id = :empresaId
		""")
	List<EmpresaModulo> findByEmpresaIdWithModulo(@Param("empresaId") Long empresaId);

	Optional<EmpresaModulo> findByEmpresaIdAndModuloId(Long empresaId, Long moduloId);
}
