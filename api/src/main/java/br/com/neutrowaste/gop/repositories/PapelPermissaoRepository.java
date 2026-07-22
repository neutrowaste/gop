package br.com.neutrowaste.gop.repositories;

import br.com.neutrowaste.gop.models.PapelPermissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PapelPermissaoRepository extends JpaRepository<PapelPermissao, Long> {

	boolean existsByPapelIdAndPermissaoId(Long papelId, Long permissaoId);

	@Query("""
		select distinct pp
		from PapelPermissao pp
		join fetch pp.permissao permissao
		left join fetch permissao.modulo modulo
		where pp.papel.id in :papelIds
		  and permissao.ativo = true
		  and (modulo is null or modulo.ativo = true)
		""")
	List<PapelPermissao> findPermissoesAtivasByPapelIds(
		@Param("papelIds") Collection<Long> papelIds
	);
}
