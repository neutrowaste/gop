package br.com.gop.repositories;

import br.com.gop.models.Ativo;
import br.com.gop.models.StatusAtivo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtivoRepository extends JpaRepository<Ativo, Long> {

	long countByEmpresaIdAndAtivoTrueAndStatus(Long empresaId, StatusAtivo status);
}
