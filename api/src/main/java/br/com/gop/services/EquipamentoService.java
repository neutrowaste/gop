package br.com.gop.services;

import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.EquipamentoRequest;
import br.com.gop.models.EquipamentoResponse;
import br.com.gop.models.FonteGps;
import br.com.gop.repositories.EquipamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("postgres")
public class EquipamentoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EquipamentoService.class);

	private final EmpresaService empresaService;
	private final EquipamentoRepository equipamentoRepository;
	private final ControleTetoAtivosService controleTetoAtivosService;
	private final AuditoriaService auditoriaService;

	public EquipamentoService(
		EmpresaService empresaService,
		EquipamentoRepository equipamentoRepository,
		ControleTetoAtivosService controleTetoAtivosService,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.equipamentoRepository = equipamentoRepository;
		this.controleTetoAtivosService = controleTetoAtivosService;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<EquipamentoResponse> listar(Long empresaId) {
		empresaService.buscarEntidade(empresaId);
		return equipamentoRepository.findByEmpresaIdOrderByCodigoAsc(empresaId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public EquipamentoResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public EquipamentoResponse criar(Long empresaId, EquipamentoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, null);
		garantirCodigoExternoUnico(empresaId, request.codigoExterno(), null);

		boolean ficaraAtivo = request.ativo() == null || request.ativo();
		if (ficaraAtivo) {
			controleTetoAtivosService.validarNovoAtivo(empresaId);
		}

		Equipamento equipamento = new Equipamento();
		equipamento.setEmpresa(empresa);
		equipamento.setCodigo(codigo);
		equipamento.setDescricao(request.descricao() == null ? null : request.descricao().strip());
		equipamento.setCodigoExterno(normalizarCodigoExterno(request.codigoExterno()));
		if (request.fonte() != null) {
			equipamento.setFonte(request.fonte());
		}
		equipamento.setAtivo(ficaraAtivo);
		equipamentoRepository.save(equipamento);

		auditoriaService.registrar(
			empresa,
			"EQUIPAMENTO_CRIADO",
			"Equipamento",
			equipamento.getId(),
			Map.of("codigo", codigo)
		);
		LOGGER.info("Equipamento criado: id={}, empresaId={}, codigo={}", equipamento.getId(), empresaId, codigo);
		return paraResponse(equipamento);
	}

	@Transactional
	public EquipamentoResponse atualizar(Long empresaId, Long id, EquipamentoRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipamento equipamento = buscarEntidade(empresaId, id);
		String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
		garantirCodigoUnico(empresaId, codigo, id);
		garantirCodigoExternoUnico(empresaId, request.codigoExterno(), id);

		boolean reativando = request.ativo() != null && request.ativo() && !equipamento.isAtivo();
		if (reativando) {
			controleTetoAtivosService.validarNovoAtivo(empresaId);
		}

		equipamento.setCodigo(codigo);
		equipamento.setDescricao(request.descricao() == null ? null : request.descricao().strip());
		equipamento.setCodigoExterno(normalizarCodigoExterno(request.codigoExterno()));
		if (request.fonte() != null) {
			equipamento.setFonte(request.fonte());
		}
		if (request.ativo() != null) {
			equipamento.setAtivo(request.ativo());
		}
		equipamentoRepository.save(equipamento);

		auditoriaService.registrar(
			empresa,
			"EQUIPAMENTO_ATUALIZADO",
			"Equipamento",
			id,
			new LinkedHashMap<>(Map.of("codigo", codigo))
		);
		LOGGER.info("Equipamento atualizado: id={}, empresaId={}", id, empresaId);
		return paraResponse(equipamento);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Equipamento equipamento = buscarEntidade(empresaId, id);
		equipamento.setAtivo(false);
		equipamentoRepository.save(equipamento);
		auditoriaService.registrar(
			empresa,
			"EQUIPAMENTO_DESATIVADO",
			"Equipamento",
			id,
			Map.of("codigo", equipamento.getCodigo())
		);
		LOGGER.info("Equipamento desativado: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Equipamento buscarEntidade(Long empresaId, Long id) {
		return equipamentoRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado"));
	}

	@Transactional(readOnly = true)
	public Equipamento resolver(Long empresaId, Long id, String codigo, String codigoExterno) {
		if (id != null) {
			return buscarEntidade(empresaId, id);
		}
		String externo = normalizarCodigoExterno(codigoExterno);
		if (externo != null) {
			return equipamentoRepository.findByEmpresaIdAndCodigoExterno(empresaId, externo)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
					"Equipamento não encontrado para codigoExterno=" + externo
				));
		}
		if (codigo != null && !codigo.isBlank()) {
			return equipamentoRepository.findByEmpresaIdAndCodigoIgnoreCase(empresaId, codigo.strip())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
					"Equipamento não encontrado para código=" + codigo.strip()
				));
		}
		throw new RegraNegocioException("Informe equipamentoId, equipamentoCodigo ou codigoExterno");
	}

	@Transactional
	public void vincularExterno(Equipamento equipamento, String codigoExterno, FonteGps fonte) {
		Equipamento gerenciado = equipamentoRepository.findById(equipamento.getId()).orElse(equipamento);
		String externo = normalizarCodigoExterno(codigoExterno);
		if (externo != null && gerenciado.getCodigoExterno() == null) {
			if (equipamentoRepository.existsByEmpresaIdAndCodigoExterno(gerenciado.getEmpresa().getId(), externo)
				&& !externo.equals(gerenciado.getCodigoExterno())) {
				return;
			}
			gerenciado.setCodigoExterno(externo);
		}
		if (fonte != null && gerenciado.getFonte() == null) {
			gerenciado.setFonte(fonte);
		}
		equipamentoRepository.save(gerenciado);
		equipamento.setCodigoExterno(gerenciado.getCodigoExterno());
		equipamento.setFonte(gerenciado.getFonte());
	}

	@Transactional(readOnly = true)
	public List<Equipamento> listarEntidades(Long empresaId) {
		return equipamentoRepository.findByEmpresaIdOrderByCodigoAsc(empresaId);
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private void garantirCodigoUnico(Long empresaId, String codigo, Long idAtual) {
		boolean existe = idAtual == null
			? equipamentoRepository.existsByEmpresaIdAndCodigoIgnoreCase(empresaId, codigo)
			: equipamentoRepository.existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(empresaId, codigo, idAtual);
		if (existe) {
			throw new ConflitoNegocioException("Já existe equipamento com este código na empresa");
		}
	}

	private void garantirCodigoExternoUnico(Long empresaId, String codigoExterno, Long idAtual) {
		String externo = normalizarCodigoExterno(codigoExterno);
		if (externo == null) {
			return;
		}
		boolean existe = idAtual == null
			? equipamentoRepository.existsByEmpresaIdAndCodigoExterno(empresaId, externo)
			: equipamentoRepository.existsByEmpresaIdAndCodigoExternoAndIdNot(empresaId, externo, idAtual);
		if (existe) {
			throw new ConflitoNegocioException("Já existe equipamento com este código externo na empresa");
		}
	}

	static String normalizarCodigoExterno(String codigoExterno) {
		if (codigoExterno == null || codigoExterno.isBlank()) {
			return null;
		}
		return codigoExterno.strip();
	}

	private EquipamentoResponse paraResponse(Equipamento equipamento) {
		return new EquipamentoResponse(
			equipamento.getId(),
			equipamento.getCodigo(),
			equipamento.getDescricao(),
			equipamento.getCodigoExterno(),
			equipamento.getFonte(),
			equipamento.isAtivo()
		);
	}
}
