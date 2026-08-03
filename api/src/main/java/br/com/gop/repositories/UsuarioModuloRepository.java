package br.com.gop.repositories;

import br.com.gop.models.UsuarioModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsuarioModuloRepository extends JpaRepository<UsuarioModulo, Long> {

	@Query("""
		select um.modulo.id
		from UsuarioModulo um
		where um.usuario.id = :usuarioId
		  and um.liberado = true
		  and um.modulo.ativo = true
		""")
	Set<Long> findModuloIdsLiberadosByUsuarioId(@Param("usuarioId") Long usuarioId);

	@Query("""
		select um
		from UsuarioModulo um
		join fetch um.modulo
		where um.usuario.id = :usuarioId
		""")
	List<UsuarioModulo> findByUsuarioIdWithModulo(@Param("usuarioId") Long usuarioId);

	Optional<UsuarioModulo> findByUsuarioIdAndModuloId(Long usuarioId, Long moduloId);

	/** Usado para checar acesso de funcionário a recursos gated por módulo (ex.: mapa operacional). */
	boolean existsByUsuarioIdAndLiberadoTrueAndModulo_AtivoTrueAndModulo_CodigoIgnoreCase(
		Long usuarioId,
		String codigoModulo
	);
}
