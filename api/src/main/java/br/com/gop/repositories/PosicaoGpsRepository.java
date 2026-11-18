package br.com.gop.repositories;

import br.com.gop.models.PosicaoGps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PosicaoGpsRepository extends JpaRepository<PosicaoGps, Long> {

	Optional<PosicaoGps> findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(Long empresaId, Long equipamentoId);

	@Query("""
		select new br.com.gop.repositories.PosicaoTrajetoConsulta(
			p.equipamento.id,
			p.geom,
			p.capturadoEm,
			p.valido
		)
		from PosicaoGps p
		where p.empresa.id = :empresaId
		  and p.equipamento.id in :equipamentoIds
		  and p.capturadoEm >= :inicio
		  and p.capturadoEm < :fim
		order by p.equipamento.id asc, p.capturadoEm asc
		""")
	List<PosicaoTrajetoConsulta> listarTrajeto(
		@Param("empresaId") Long empresaId,
		@Param("equipamentoIds") Collection<Long> equipamentoIds,
		@Param("inicio") Instant inicio,
		@Param("fim") Instant fim
	);
}
