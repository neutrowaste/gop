package br.com.gop.services;

import br.com.gop.models.CategoriaModulo;
import br.com.gop.models.Empresa;
import br.com.gop.models.EmpresaModulo;
import br.com.gop.models.Modulo;
import br.com.gop.models.Usuario;
import br.com.gop.models.UsuarioModulo;
import br.com.gop.models.UsuarioModuloRequest;
import br.com.gop.models.UsuarioModuloResponse;
import br.com.gop.repositories.EmpresaModuloRepository;
import br.com.gop.repositories.ModuloRepository;
import br.com.gop.repositories.UsuarioModuloRepository;
import br.com.gop.repositories.UsuarioRepository;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("postgres")
public class UsuarioModuloService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioModuloService.class);

	private final EmpresaService empresaService;
	private final UsuarioRepository usuarioRepository;
	private final ModuloRepository moduloRepository;
	private final EmpresaModuloRepository empresaModuloRepository;
	private final UsuarioModuloRepository usuarioModuloRepository;
	private final AuditoriaService auditoriaService;

	public UsuarioModuloService(
		EmpresaService empresaService,
		UsuarioRepository usuarioRepository,
		ModuloRepository moduloRepository,
		EmpresaModuloRepository empresaModuloRepository,
		UsuarioModuloRepository usuarioModuloRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.usuarioRepository = usuarioRepository;
		this.moduloRepository = moduloRepository;
		this.empresaModuloRepository = empresaModuloRepository;
		this.usuarioModuloRepository = usuarioModuloRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<UsuarioModuloResponse> listarPorUsuario(Long empresaId, Long usuarioId) {
		empresaService.buscarEntidade(empresaId);
		Usuario usuario = buscarUsuario(empresaId, usuarioId);

		Set<Long> contratados = empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(empresaId);
		Map<Long, UsuarioModulo> liberacoes = usuarioModuloRepository
			.findByUsuarioIdWithModulo(usuario.getId())
			.stream()
			.collect(Collectors.toMap(v -> v.getModulo().getId(), Function.identity()));

		List<UsuarioModuloResponse> resposta = new ArrayList<>();
		for (Modulo modulo : moduloRepository.findByAtivoTrueOrderByOrdemAscNomeAsc()) {
			boolean contratado = contratados.contains(modulo.getId());
			UsuarioModulo vinculo = liberacoes.get(modulo.getId());
			boolean liberado = contratado && vinculo != null && vinculo.isLiberado();
			resposta.add(new UsuarioModuloResponse(
				modulo.getId(),
				modulo.getCodigo(),
				modulo.getNome(),
				modulo.getCategoria() == null ? CategoriaModulo.TRANSVERSAL : modulo.getCategoria(),
				modulo.getOrdem(),
				contratado,
				liberado,
				vinculo == null ? null : vinculo.getDesde()
			));
		}
		return resposta;
	}

	@Transactional(readOnly = true)
	public List<UsuarioModuloResponse> listarLiberados(Long empresaId, Long usuarioId) {
		return listarPorUsuario(empresaId, usuarioId).stream()
			.filter(UsuarioModuloResponse::liberado)
			.toList();
	}

	/**
	 * Checa se o usuário tem o módulo (código) liberado, sem exigir papel de
	 * admin — usado por recursos operacionais acessíveis a funcionários
	 * comuns quando o módulo correspondente está liberado (ex.: mapa
	 * operacional gated pelo módulo MONITORAMENTO).
	 */
	@Transactional(readOnly = true)
	public boolean possuiModuloLiberado(Long usuarioId, String codigoModulo) {
		return usuarioModuloRepository
			.existsByUsuarioIdAndLiberadoTrueAndModulo_AtivoTrueAndModulo_CodigoIgnoreCase(usuarioId, codigoModulo);
	}

	@Transactional
	public UsuarioModuloResponse atualizar(
		Long empresaId,
		Long usuarioId,
		Long moduloId,
		UsuarioModuloRequest request
	) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Não é possível alterar módulos de empresa inativa");
		}

		Usuario usuario = buscarUsuario(empresaId, usuarioId);
		Modulo modulo = moduloRepository.findById(moduloId)
			.filter(Modulo::isAtivo)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado"));

		boolean liberar = Boolean.TRUE.equals(request.liberado());
		if (liberar && !empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(empresaId)
			.contains(moduloId)) {
			throw new RegraNegocioException(
				"Só é possível liberar módulos já contratados pela organização"
			);
		}

		UsuarioModulo vinculo = usuarioModuloRepository
			.findByUsuarioIdAndModuloId(usuarioId, moduloId)
			.orElseGet(() -> {
				UsuarioModulo novo = new UsuarioModulo();
				novo.setUsuario(usuario);
				novo.setModulo(modulo);
				novo.setLiberado(false);
				return novo;
			});

		boolean anterior = vinculo.isLiberado();
		vinculo.setLiberado(liberar);
		if (liberar && vinculo.getDesde() == null) {
			vinculo.setDesde(Instant.now());
		}
		usuarioModuloRepository.save(vinculo);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("usuarioId", usuarioId);
		payload.put("moduloCodigo", modulo.getCodigo());
		payload.put("anterior", anterior);
		payload.put("liberado", liberar);
		auditoriaService.registrar(
			empresa,
			liberar ? "MODULO_LIBERADO_USUARIO" : "MODULO_REVOGADO_USUARIO",
			"UsuarioModulo",
			vinculo.getId(),
			payload
		);
		LOGGER.info(
			"Módulo {} {} para usuarioId={} empresaId={}",
			modulo.getCodigo(),
			liberar ? "liberado" : "revogado",
			usuarioId,
			empresaId
		);

		return new UsuarioModuloResponse(
			modulo.getId(),
			modulo.getCodigo(),
			modulo.getNome(),
			modulo.getCategoria() == null ? CategoriaModulo.TRANSVERSAL : modulo.getCategoria(),
			modulo.getOrdem(),
			true,
			vinculo.isLiberado(),
			vinculo.getDesde()
		);
	}

	private Usuario buscarUsuario(Long empresaId, Long usuarioId) {
		return usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}
}
