package br.com.gop.repositories;

import br.com.gop.models.Trecho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrechoRepository extends JpaRepository<Trecho, Long> {

	@Query("SELECT t FROM Trecho t JOIN FETCH t.setor WHERE t.empresa.id = :empresaId ORDER BY t.codigo ASC")
	List<Trecho> findByEmpresaIdOrderByCodigoAsc(@Param("empresaId") Long empresaId);

	@Query("SELECT t FROM Trecho t JOIN FETCH t.setor WHERE t.empresa.id = :empresaId AND t.setor.id = :setorId ORDER BY t.codigo ASC")
	List<Trecho> findByEmpresaIdAndSetorIdOrderByCodigoAsc(
		@Param("empresaId") Long empresaId,
		@Param("setorId") Long setorId
	);

	@Query("SELECT t FROM Trecho t JOIN FETCH t.setor WHERE t.id = :id AND t.empresa.id = :empresaId")
	Optional<Trecho> findByIdAndEmpresaId(@Param("id") Long id, @Param("empresaId") Long empresaId);

	boolean existsByEmpresaIdAndCodigoIgnoreCase(Long empresaId, String codigo);

	boolean existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(Long empresaId, String codigo, Long id);
}
