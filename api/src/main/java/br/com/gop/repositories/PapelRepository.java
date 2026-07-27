package br.com.gop.repositories;

import br.com.gop.models.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PapelRepository extends JpaRepository<Papel, Long> {

	Optional<Papel> findByCodigoIgnoreCaseAndEmpresaIsNull(String codigo);
}
