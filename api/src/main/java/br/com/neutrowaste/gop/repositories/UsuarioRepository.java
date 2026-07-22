package br.com.neutrowaste.gop.repositories;

import br.com.neutrowaste.gop.models.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	@EntityGraph(attributePaths = "empresa")
	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<Usuario> findByEmpresaIdOrderByNomeAsc(Long empresaId);

	Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);
}
