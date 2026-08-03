package br.com.gop.repositories;

import br.com.gop.models.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipeRepository extends JpaRepository<Equipe, Long> {

	List<Equipe> findByEmpresaIdOrderByNomeAsc(Long empresaId);

	List<Equipe> findByEmpresaIdAndSetorIdOrderByNomeAsc(Long empresaId, Long setorId);

	Optional<Equipe> findByIdAndEmpresaId(Long id, Long empresaId);
}
