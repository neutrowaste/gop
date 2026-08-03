package br.com.gop.repositories;

import br.com.gop.models.Trecho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrechoRepository extends JpaRepository<Trecho, Long> {

	List<Trecho> findByEmpresaIdOrderByCodigoAsc(Long empresaId);

	List<Trecho> findByEmpresaIdAndRuaIdOrderByCodigoAsc(Long empresaId, Long ruaId);

	Optional<Trecho> findByIdAndEmpresaId(Long id, Long empresaId);

	boolean existsByEmpresaIdAndCodigoIgnoreCase(Long empresaId, String codigo);

	boolean existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(Long empresaId, String codigo, Long id);
}
