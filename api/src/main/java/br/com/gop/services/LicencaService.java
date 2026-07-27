package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Licenca;
import br.com.gop.models.LicencaRequest;
import br.com.gop.models.LicencaResponse;
import br.com.gop.models.StatusLicenca;
import br.com.gop.repositories.LicencaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("postgres")
public class LicencaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LicencaService.class);

	private final LicencaRepository licencaRepository;
	private final EmpresaService empresaService;
	private final ControleTetoAtivosService controleTetoAtivosService;
	private final AuditoriaService auditoriaService;

	public LicencaService(
		LicencaRepository licencaRepository,
		EmpresaService empresaService,
		ControleTetoAtivosService controleTetoAtivosService,
		AuditoriaService auditoriaService
	) {
		this.licencaRepository = licencaRepository;
		this.empresaService = empresaService;
		this.controleTetoAtivosService = controleTetoAtivosService;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<LicencaResponse> listarPorEmpresa(Long empresaId) {
		empresaService.buscarEntidade(empresaId);
		return licencaRepository.findByEmpresaIdOrderByVigenciaInicioDesc(empresaId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public LicencaResponse buscar(Long id) {
		return paraResponse(buscarEntidade(id));
	}

	@Transactional
	public LicencaResponse criar(Long empresaId, LicencaRequest request) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Não é possível licenciar uma empresa inativa");
		}
		validarRequest(request);
		validarSobreposicao(empresaId, request, null);
		if (request.status() == StatusLicenca.ATIVA && ativo(request)) {
			controleTetoAtivosService.validarNovoTeto(empresaId, request.tetoAtivos());
		}

		Licenca licenca = new Licenca();
		licenca.setEmpresa(empresa);
		aplicar(licenca, request);
		licencaRepository.save(licenca);
		auditoriaService.registrar(
			empresa,
			"LICENCA_CRIADA",
			"Licenca",
			licenca.getId(),
			payloadLicenca(licenca)
		);
		LOGGER.info(
			"Licença criada: licencaId={}, empresaId={}, status={}, tetoAtivos={}, vigencia={}..{}",
			licenca.getId(),
			empresaId,
			licenca.getStatus(),
			licenca.getTetoAtivos(),
			licenca.getVigenciaInicio(),
			licenca.getVigenciaFim()
		);
		return paraResponse(licenca);
	}

	@Transactional
	public LicencaResponse atualizar(Long id, LicencaRequest request) {
		Licenca licenca = buscarEntidade(id);
		validarRequest(request);
		validarSobreposicao(licenca.getEmpresa().getId(), request, id);
		if (request.status() == StatusLicenca.ATIVA && ativo(request)) {
			controleTetoAtivosService.validarNovoTeto(
				licenca.getEmpresa().getId(),
				request.tetoAtivos()
			);
		}

		Map<String, Object> anterior = payloadLicenca(licenca);
		aplicar(licenca, request);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("anterior", anterior);
		payload.put("novo", payloadLicenca(licenca));
		auditoriaService.registrar(
			licenca.getEmpresa(),
			"LICENCA_ATUALIZADA",
			"Licenca",
			licenca.getId(),
			payload
		);
		LOGGER.info(
			"Licença atualizada: licencaId={}, empresaId={}, status={}, tetoAtivos={}",
			licenca.getId(),
			licenca.getEmpresa().getId(),
			licenca.getStatus(),
			licenca.getTetoAtivos()
		);
		return paraResponse(licenca);
	}

	@Transactional
	public void encerrar(Long id) {
		Licenca licenca = buscarEntidade(id);
		if (!licenca.isAtivo() && licenca.getStatus() == StatusLicenca.ENCERRADA) {
			return;
		}
		Map<String, Object> anterior = payloadLicenca(licenca);
		licenca.setStatus(StatusLicenca.ENCERRADA);
		licenca.setAtivo(false);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("anterior", anterior);
		payload.put("novo", payloadLicenca(licenca));
		auditoriaService.registrar(
			licenca.getEmpresa(),
			"LICENCA_ENCERRADA",
			"Licenca",
			licenca.getId(),
			payload
		);
		LOGGER.info(
			"Licença encerrada: licencaId={}, empresaId={}",
			licenca.getId(),
			licenca.getEmpresa().getId()
		);
	}

	private Licenca buscarEntidade(Long id) {
		return licencaRepository.findComEmpresaById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Licença não encontrada"));
	}

	private void validarRequest(LicencaRequest request) {
		if (request.vigenciaFim() != null
			&& request.vigenciaFim().isBefore(request.vigenciaInicio())) {
			throw new RegraNegocioException(
				"A data final da licença não pode ser anterior à data inicial"
			);
		}
	}

	private void validarSobreposicao(Long empresaId, LicencaRequest request, Long ignorarId) {
		if (!ativo(request) || request.status() == StatusLicenca.ENCERRADA) {
			return;
		}
		if (licencaRepository.existeSobreposicao(
			empresaId,
			request.vigenciaInicio(),
			request.vigenciaFim(),
			ignorarId,
			StatusLicenca.ENCERRADA
		)) {
			throw new ConflitoNegocioException(
				"Já existe uma licença ativa ou suspensa no período informado"
			);
		}
	}

	private void aplicar(Licenca licenca, LicencaRequest request) {
		licenca.setTetoAtivos(request.tetoAtivos());
		licenca.setVigenciaInicio(request.vigenciaInicio());
		licenca.setVigenciaFim(request.vigenciaFim());
		licenca.setStatus(request.status());
		licenca.setAtivo(ativo(request));
	}

	private boolean ativo(LicencaRequest request) {
		return request.ativo() == null || request.ativo();
	}

	private LicencaResponse paraResponse(Licenca licenca) {
		return new LicencaResponse(
			licenca.getId(),
			licenca.getEmpresa().getId(),
			licenca.getEmpresa().getCodigo(),
			licenca.getTetoAtivos(),
			licenca.getVigenciaInicio(),
			licenca.getVigenciaFim(),
			licenca.getStatus(),
			licenca.isAtivo(),
			licenca.getCriadoEm(),
			licenca.getAtualizadoEm()
		);
	}

	private Map<String, Object> payloadLicenca(Licenca licenca) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("tetoAtivos", licenca.getTetoAtivos());
		payload.put("vigenciaInicio", licenca.getVigenciaInicio());
		payload.put("vigenciaFim", licenca.getVigenciaFim());
		payload.put("status", licenca.getStatus().name());
		payload.put("ativo", licenca.isAtivo());
		return payload;
	}
}
