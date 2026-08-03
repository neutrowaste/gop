package br.com.gop.repositories;

import br.com.gop.models.Ocorrencia;
import br.com.gop.models.StatusOcorrencia;
import br.com.gop.models.TipoOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long>, JpaSpecificationExecutor<Ocorrencia> {

	Optional<Ocorrencia> findByIdAndEmpresaId(Long id, Long empresaId);

	boolean existsByTrechoExecucaoIdAndTipoAndStatus(Long trechoExecucaoId, TipoOcorrencia tipo, StatusOcorrencia status);

	Optional<Ocorrencia> findByEscalaIdAndEquipamentoIdAndTipoAndStatus(
		Long escalaId,
		Long equipamentoId,
		TipoOcorrencia tipo,
		StatusOcorrencia status
	);

	boolean existsByEscalaIdAndEquipamentoIdAndTipoAndStatus(
		Long escalaId,
		Long equipamentoId,
		TipoOcorrencia tipo,
		StatusOcorrencia status
	);
}
