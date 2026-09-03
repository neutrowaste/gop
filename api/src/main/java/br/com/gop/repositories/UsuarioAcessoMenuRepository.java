package br.com.gop.repositories;

import br.com.gop.models.UsuarioAcessoMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsuarioAcessoMenuRepository extends JpaRepository<UsuarioAcessoMenu, Long> {

	List<UsuarioAcessoMenu> findByUsuarioId(Long usuarioId);

	Optional<UsuarioAcessoMenu> findByUsuarioIdAndCodigoIgnoreCase(Long usuarioId, String codigo);

	boolean existsByUsuarioIdAndCodigoIgnoreCaseAndLiberadoTrue(Long usuarioId, String codigo);

	boolean existsByUsuarioIdAndLiberadoTrueAndCodigoIgnoreCaseIn(Long usuarioId, Collection<String> codigos);

	@Query("""
		select upper(ua.codigo)
		from UsuarioAcessoMenu ua
		where ua.usuario.id = :usuarioId
		  and ua.liberado = true
		""")
	Set<String> findCodigosLiberadosByUsuarioId(@Param("usuarioId") Long usuarioId);
}
