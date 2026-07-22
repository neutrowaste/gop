package br.com.neutrowaste.gop.repositories;

import br.com.neutrowaste.gop.models.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {

	Optional<Modulo> findByCodigoIgnoreCase(String codigo);

	List<Modulo> findByAtivoTrueOrderByOrdemAscNomeAsc();
}
