package br.com.gop.repositories;

import br.com.gop.models.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	@EntityGraph(attributePaths = "empresa")
	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<Usuario> findByEmpresaIdOrderByNomeAsc(Long empresaId);

	Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);

	@Query("""
		select distinct u
		from Usuario u
		join UsuarioPapel up on up.usuario = u
		join up.papel p
		where u.empresa.id = :empresaId
		  and upper(p.codigo) = upper(:codigoPapel)
		order by u.nome asc
		""")
	List<Usuario> findByEmpresaIdAndPapelCodigo(
		@Param("empresaId") Long empresaId,
		@Param("codigoPapel") String codigoPapel
	);
}
