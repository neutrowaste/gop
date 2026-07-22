package br.com.neutrowaste.gop.repositories;

import br.com.neutrowaste.gop.models.Ativo;
import br.com.neutrowaste.gop.models.StatusAtivo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtivoRepository extends JpaRepository<Ativo, Long> {

	long countByEmpresaIdAndAtivoTrueAndStatus(Long empresaId, StatusAtivo status);
}
