package br.com.gop.repositories;

import br.com.gop.models.Rua;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuaRepository extends JpaRepository<Rua, Long> {

	List<Rua> findByEmpresaIdOrderByNomeAsc(Long empresaId);

	List<Rua> findByEmpresaIdAndCidadeIdOrderByNomeAsc(Long empresaId, Long cidadeId);

	Optional<Rua> findByIdAndEmpresaId(Long id, Long empresaId);
}
