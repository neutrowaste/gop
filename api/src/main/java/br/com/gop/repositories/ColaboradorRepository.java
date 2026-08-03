package br.com.gop.repositories;

import br.com.gop.models.Colaborador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {

	List<Colaborador> findByEmpresaIdOrderByNomeAsc(Long empresaId);

	List<Colaborador> findByEmpresaIdAndEquipeIdOrderByNomeAsc(Long empresaId, Long equipeId);

	Optional<Colaborador> findByIdAndEmpresaId(Long id, Long empresaId);

	boolean existsByEmpresaIdAndMatriculaIgnoreCase(Long empresaId, String matricula);

	boolean existsByEmpresaIdAndMatriculaIgnoreCaseAndIdNot(Long empresaId, String matricula, Long id);
}
