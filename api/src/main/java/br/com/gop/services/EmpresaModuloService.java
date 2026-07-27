package br.com.gop.services;

import br.com.gop.models.CategoriaModulo;
import br.com.gop.models.Empresa;
import br.com.gop.models.EmpresaModulo;
import br.com.gop.models.EmpresaModuloRequest;
import br.com.gop.models.EmpresaModuloResponse;
import br.com.gop.models.Modulo;
import br.com.gop.repositories.EmpresaModuloRepository;
import br.com.gop.repositories.ModuloRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("postgres")
public class EmpresaModuloService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmpresaModuloService.class);

	private final EmpresaService empresaService;
	private final ModuloRepository moduloRepository;
	private final EmpresaModuloRepository empresaModuloRepository;
	private final AuditoriaService auditoriaService;

	public EmpresaModuloService(
		EmpresaService empresaService,
		ModuloRepository moduloRepository,
		EmpresaModuloRepository empresaModuloRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.moduloRepository = moduloRepository;
		this.empresaModuloRepository = empresaModuloRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<EmpresaModuloResponse> listarPorEmpresa(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		Map<Long, EmpresaModulo> vinculos = empresaModuloRepository
			.findByEmpresaIdWithModulo(empresa.getId())
			.stream()
			.collect(Collectors.toMap(v -> v.getModulo().getId(), Function.identity()));

		List<EmpresaModuloResponse> resposta = new ArrayList<>();
		for (Modulo modulo : moduloRepository.findByAtivoTrueOrderByOrdemAscNomeAsc()) {
			EmpresaModulo vinculo = vinculos.get(modulo.getId());
			resposta.add(new EmpresaModuloResponse(
				modulo.getId(),
				modulo.getCodigo(),
				modulo.getNome(),
				modulo.getCategoria(),
				modulo.getOrdem(),
				vinculo != null && vinculo.isHabilitado(),
				vinculo == null ? null : vinculo.getDesde()
			));
		}
		return resposta;
	}

	@Transactional
	public EmpresaModuloResponse atualizar(
		Long empresaId,
		Long moduloId,
		EmpresaModuloRequest request
	) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Não é possível alterar módulos de empresa inativa");
		}

		Modulo modulo = moduloRepository.findById(moduloId)
			.filter(Modulo::isAtivo)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado"));

		boolean habilitar = Boolean.TRUE.equals(request.habilitado());
		EmpresaModulo vinculo = empresaModuloRepository
			.findByEmpresaIdAndModuloId(empresaId, moduloId)
			.orElseGet(() -> {
				EmpresaModulo novo = new EmpresaModulo();
				novo.setEmpresa(empresa);
				novo.setModulo(modulo);
				novo.setHabilitado(false);
				return novo;
			});

		boolean anterior = vinculo.isHabilitado();
		vinculo.setHabilitado(habilitar);
		if (habilitar && vinculo.getDesde() == null) {
			vinculo.setDesde(Instant.now());
		}
		empresaModuloRepository.save(vinculo);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("moduloCodigo", modulo.getCodigo());
		payload.put("anterior", anterior);
		payload.put("habilitado", habilitar);
		auditoriaService.registrar(
			empresa,
			habilitar ? "MODULO_HABILITADO" : "MODULO_DESABILITADO",
			"EmpresaModulo",
			vinculo.getId(),
			payload
		);
		LOGGER.info(
			"Módulo {} {} para empresaId={}",
			modulo.getCodigo(),
			habilitar ? "habilitado" : "desabilitado",
			empresaId
		);

		return new EmpresaModuloResponse(
			modulo.getId(),
			modulo.getCodigo(),
			modulo.getNome(),
			modulo.getCategoria() == null ? CategoriaModulo.TRANSVERSAL : modulo.getCategoria(),
			modulo.getOrdem(),
			vinculo.isHabilitado(),
			vinculo.getDesde()
		);
	}
}
