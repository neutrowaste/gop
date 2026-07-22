package br.com.neutrowaste.gop.repositories;

import br.com.neutrowaste.gop.models.UsuarioPapel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioPapelRepository extends JpaRepository<UsuarioPapel, Long> {

	@Query("""
		select count(up) > 0
		from UsuarioPapel up
		where up.usuario.id = :usuarioId
		  and up.papel.id = :papelId
		  and up.empresa is null
		""")
	boolean existsVinculoPlataforma(
		@Param("usuarioId") Long usuarioId,
		@Param("papelId") Long papelId
	);

	@Query("""
		select up
		from UsuarioPapel up
		join fetch up.papel p
		left join fetch p.empresa
		left join fetch up.empresa
		left join fetch up.filial filial
		left join fetch filial.empresa
		where up.usuario.id = :usuarioId
		  and p.ativo = true
		""")
	List<UsuarioPapel> findPapeisAtivosByUsuarioId(@Param("usuarioId") Long usuarioId);

	@Query("""
		select count(up) > 0
		from UsuarioPapel up
		where up.usuario.id = :usuarioId
		  and up.papel.id = :papelId
		  and up.empresa.id = :empresaId
		""")
	boolean existsVinculoEmpresa(
		@Param("usuarioId") Long usuarioId,
		@Param("papelId") Long papelId,
		@Param("empresaId") Long empresaId
	);
}
