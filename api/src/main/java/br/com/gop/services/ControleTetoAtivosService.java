package br.com.gop.services;

import br.com.gop.models.Licenca;
import br.com.gop.models.StatusLicenca;
import br.com.gop.models.TetoAtivosResponse;
import br.com.gop.repositories.EquipamentoRepository;
import br.com.gop.repositories.LicencaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Controla o teto contratado de GPS/Lutocares por empresa (RN faturamento por ativo).
 * Na Fase 1 o consumo é o cadastro de Equipamento (Lutocar) ativo.
 */
@Service
@Profile("postgres")
public class ControleTetoAtivosService {

	private final LicencaRepository licencaRepository;
	private final EquipamentoRepository equipamentoRepository;

	public ControleTetoAtivosService(
		LicencaRepository licencaRepository,
		EquipamentoRepository equipamentoRepository
	) {
		this.licencaRepository = licencaRepository;
		this.equipamentoRepository = equipamentoRepository;
	}

	@Transactional(readOnly = true)
	public TetoAtivosResponse consultar(Long empresaId) {
		Licenca licenca = buscarLicencaVigente(empresaId);
		long ativosEmUso = contarAtivosEmUso(empresaId);
		long saldo = Math.max(0, (long) licenca.getTetoAtivos() - ativosEmUso);
		return new TetoAtivosResponse(
			empresaId,
			licenca.getId(),
			licenca.getTetoAtivos(),
			ativosEmUso,
			saldo,
			saldo > 0
		);
	}

	@Transactional(readOnly = true)
	public void validarNovoAtivo(Long empresaId) {
		TetoAtivosResponse teto = consultar(empresaId);
		if (!teto.permiteNovoAtivo()) {
			throw new RegraNegocioException(
				"Teto de Lutocares (GPS) contratado foi atingido ("
					+ teto.ativosEmUso() + "/" + teto.tetoAtivos() + ")"
			);
		}
	}

	@Transactional(readOnly = true)
	public void validarNovoTeto(Long empresaId, int novoTeto) {
		long ativosEmUso = contarAtivosEmUso(empresaId);
		if (novoTeto < ativosEmUso) {
			throw new RegraNegocioException(
				"O teto não pode ser menor que a quantidade de Lutocares (GPS) em uso: " + ativosEmUso
			);
		}
	}

	private Licenca buscarLicencaVigente(Long empresaId) {
		return licencaRepository.findVigente(empresaId, StatusLicenca.ATIVA, LocalDate.now())
			.orElseThrow(() -> new RegraNegocioException("A empresa não possui licença vigente"));
	}

	private long contarAtivosEmUso(Long empresaId) {
		return equipamentoRepository.countByEmpresaIdAndAtivoTrue(empresaId);
	}
}
