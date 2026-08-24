package br.com.gop.repositories;

import br.com.gop.models.Equipamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

	List<Equipamento> findByEmpresaIdOrderByCodigoAsc(Long empresaId);

	Optional<Equipamento> findByIdAndEmpresaId(Long id, Long empresaId);

	boolean existsByEmpresaIdAndCodigoIgnoreCase(Long empresaId, String codigo);

	boolean existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(Long empresaId, String codigo, Long id);

	Optional<Equipamento> findByEmpresaIdAndCodigoIgnoreCase(Long empresaId, String codigo);

	Optional<Equipamento> findByEmpresaIdAndCodigoExterno(Long empresaId, String codigoExterno);

	boolean existsByEmpresaIdAndCodigoExternoAndIdNot(Long empresaId, String codigoExterno, Long id);

	boolean existsByEmpresaIdAndCodigoExterno(Long empresaId, String codigoExterno);

	long countByEmpresaIdAndAtivoTrue(Long empresaId);
}
