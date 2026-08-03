package br.com.gop.repositories;

import br.com.gop.models.ParametroEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParametroEmpresaRepository extends JpaRepository<ParametroEmpresa, Long> {

	List<ParametroEmpresa> findByEmpresaIdOrderByChaveAsc(Long empresaId);

	Optional<ParametroEmpresa> findByEmpresaIdAndChaveAndAtivoTrue(Long empresaId, String chave);

	Optional<ParametroEmpresa> findByEmpresaIdAndChave(Long empresaId, String chave);
}
