package br.com.gop.repositories;

import br.com.gop.models.PosicaoGps;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PosicaoGpsRepository extends JpaRepository<PosicaoGps, Long> {

	Optional<PosicaoGps> findTopByEmpresaIdAndEquipamentoIdOrderByRecebidoEmDesc(Long empresaId, Long equipamentoId);
}
