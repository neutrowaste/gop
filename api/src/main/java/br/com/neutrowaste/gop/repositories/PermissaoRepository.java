package br.com.neutrowaste.gop.repositories;

import br.com.neutrowaste.gop.models.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissaoRepository extends JpaRepository<Permissao, Long> {

	Optional<Permissao> findByCodigoIgnoreCase(String codigo);
}
