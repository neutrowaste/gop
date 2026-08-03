package br.com.gop.repositories;

import br.com.gop.models.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CidadeRepository extends JpaRepository<Cidade, Long> {

	List<Cidade> findByEmpresaIdOrderByNomeAsc(Long empresaId);

	Optional<Cidade> findByIdAndEmpresaId(Long id, Long empresaId);

	boolean existsByEmpresaIdAndNomeIgnoreCaseAndUfIgnoreCase(Long empresaId, String nome, String uf);

	boolean existsByEmpresaIdAndNomeIgnoreCaseAndUfIgnoreCaseAndIdNot(
		Long empresaId,
		String nome,
		String uf,
		Long id
	);
}
