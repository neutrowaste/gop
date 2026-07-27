package br.com.gop.repositories;

import br.com.gop.models.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

	boolean existsByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

	List<Empresa> findAllByOrderByNomeAsc();

	List<Empresa> findByAtivoOrderByNomeAsc(boolean ativo);
}
