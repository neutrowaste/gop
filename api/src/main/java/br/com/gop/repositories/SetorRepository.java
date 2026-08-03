package br.com.gop.repositories;

import br.com.gop.models.Setor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SetorRepository extends JpaRepository<Setor, Long> {

	List<Setor> findByEmpresaIdOrderByNomeAsc(Long empresaId);

	List<Setor> findByEmpresaIdAndCidadeIdOrderByNomeAsc(Long empresaId, Long cidadeId);

	Optional<Setor> findByIdAndEmpresaId(Long id, Long empresaId);

	boolean existsByEmpresaIdAndCodigoIgnoreCase(Long empresaId, String codigo);

	boolean existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(Long empresaId, String codigo, Long id);
}
