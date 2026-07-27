package br.com.gop.repositories;

import br.com.gop.models.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissaoRepository extends JpaRepository<Permissao, Long> {

	Optional<Permissao> findByCodigoIgnoreCase(String codigo);
}
