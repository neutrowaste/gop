package br.com.gop.services;

import br.com.gop.models.Licenca;
import br.com.gop.models.StatusAtivo;
import br.com.gop.models.StatusLicenca;
import br.com.gop.models.TetoAtivosResponse;
import br.com.gop.repositories.AtivoRepository;
import br.com.gop.repositories.LicencaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Profile("postgres")
public class ControleTetoAtivosService {

	private final LicencaRepository licencaRepository;
	private final AtivoRepository ativoRepository;

	public ControleTetoAtivosService(
		LicencaRepository licencaRepository,
		AtivoRepository ativoRepository
	) {
		this.licencaRepository = licencaRepository;
		this.ativoRepository = ativoRepository;
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
			throw new RegraNegocioException("Teto de ativos contratado foi atingido");
		}
	}

	@Transactional(readOnly = true)
	public void validarNovoTeto(Long empresaId, int novoTeto) {
		long ativosEmUso = contarAtivosEmUso(empresaId);
		if (novoTeto < ativosEmUso) {
			throw new RegraNegocioException(
				"O teto não pode ser menor que a quantidade de ativos em uso: " + ativosEmUso
			);
		}
	}

	private Licenca buscarLicencaVigente(Long empresaId) {
		return licencaRepository.findVigente(empresaId, StatusLicenca.ATIVA, LocalDate.now())
			.orElseThrow(() -> new RegraNegocioException("A empresa não possui licença vigente"));
	}

	private long contarAtivosEmUso(Long empresaId) {
		return ativoRepository.countByEmpresaIdAndAtivoTrueAndStatus(
			empresaId,
			StatusAtivo.ATIVO
		);
	}
}
