package br.com.gop.services;

import br.com.gop.config.CatalogoPlataformaSeedRunner;
import br.com.gop.models.AcessoMenu;
import br.com.gop.models.Empresa;
import br.com.gop.models.Usuario;
import br.com.gop.models.UsuarioAcessoMenu;
import br.com.gop.models.UsuarioAcessoMenuRequest;
import br.com.gop.models.UsuarioAcessoMenuResponse;
import br.com.gop.repositories.UsuarioAcessoMenuRepository;
import br.com.gop.repositories.UsuarioPapelRepository;
import br.com.gop.repositories.UsuarioRepository;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.security.UsuarioTokenPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("acessoMenuService")
@Profile("postgres")
public class AcessoMenuService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AcessoMenuService.class);

	private final EmpresaService empresaService;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioPapelRepository usuarioPapelRepository;
	private final UsuarioAcessoMenuRepository usuarioAcessoMenuRepository;
	private final AuditoriaService auditoriaService;

	public AcessoMenuService(
		EmpresaService empresaService,
		UsuarioRepository usuarioRepository,
		UsuarioPapelRepository usuarioPapelRepository,
		UsuarioAcessoMenuRepository usuarioAcessoMenuRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.usuarioRepository = usuarioRepository;
		this.usuarioPapelRepository = usuarioPapelRepository;
		this.usuarioAcessoMenuRepository = usuarioAcessoMenuRepository;
		this.auditoriaService = auditoriaService;
	}

	public boolean usuarioDaEmpresa() {
		try {
			return SegurancaContexto.principalAtual().empresaId() != null;
		} catch (AccessDeniedException excecao) {
			return false;
		}
	}

	public boolean temAcesso(String codigo) {
		UsuarioTokenPrincipal principal;
		try {
			principal = SegurancaContexto.principalAtual();
		} catch (AccessDeniedException excecao) {
			return false;
		}
		if (ehAdminEmpresa(principal)) {
			return true;
		}
		AcessoMenu item = AcessoMenu.buscar(codigo).orElse(null);
		if (item == null || principal.usuarioId() == null) {
			return false;
		}
		return usuarioAcessoMenuRepository.existsByUsuarioIdAndCodigoIgnoreCaseAndLiberadoTrue(
			principal.usuarioId(),
			item.name()
		);
	}

	public boolean temQualquer(String... codigos) {
		UsuarioTokenPrincipal principal;
		try {
			principal = SegurancaContexto.principalAtual();
		} catch (AccessDeniedException excecao) {
			return false;
		}
		if (ehAdminEmpresa(principal)) {
			return true;
		}
		if (principal.usuarioId() == null || codigos == null || codigos.length == 0) {
			return false;
		}
		List<String> validos = new ArrayList<>();
		for (String codigo : codigos) {
			AcessoMenu.buscar(codigo).ifPresent(item -> validos.add(item.name()));
		}
		if (validos.isEmpty()) {
			return false;
		}
		return usuarioAcessoMenuRepository.existsByUsuarioIdAndLiberadoTrueAndCodigoIgnoreCaseIn(
			principal.usuarioId(),
			validos
		);
	}

	@Transactional(readOnly = true)
	public List<UsuarioAcessoMenuResponse> listarPorUsuario(Long empresaId, Long usuarioId) {
		empresaService.buscarEntidade(empresaId);
		Usuario usuario = buscarUsuario(empresaId, usuarioId);
		boolean admin = ehAdminEmpresa(usuario);
		return montarCatalogo(usuario.getId(), admin);
	}

	@Transactional(readOnly = true)
	public List<UsuarioAcessoMenuResponse> listarMeus(Long empresaId, Long usuarioId, boolean adminEmpresa) {
		empresaService.buscarEntidade(empresaId);
		buscarUsuario(empresaId, usuarioId);
		return montarCatalogo(usuarioId, adminEmpresa);
	}

	@Transactional
	public UsuarioAcessoMenuResponse atualizar(
		Long empresaId,
		Long usuarioId,
		String codigo,
		UsuarioAcessoMenuRequest request
	) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Não é possível alterar acessos de empresa inativa");
		}

		Usuario usuario = buscarUsuario(empresaId, usuarioId);
		AcessoMenu item = AcessoMenu.buscar(codigo)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Acesso de menu não encontrado"));

		if (ehAdminEmpresa(usuario)) {
			throw new RegraNegocioException(
				"Administrador da empresa já possui todos os acessos do painel"
			);
		}

		boolean liberar = Boolean.TRUE.equals(request.liberado());
		UsuarioAcessoMenu vinculo = usuarioAcessoMenuRepository
			.findByUsuarioIdAndCodigoIgnoreCase(usuarioId, item.name())
			.orElseGet(() -> {
				UsuarioAcessoMenu novo = new UsuarioAcessoMenu();
				novo.setUsuario(usuario);
				novo.setCodigo(item.name());
				novo.setLiberado(false);
				return novo;
			});

		boolean anterior = vinculo.isLiberado();
		vinculo.setLiberado(liberar);
		if (liberar && vinculo.getDesde() == null) {
			vinculo.setDesde(Instant.now());
		}
		usuarioAcessoMenuRepository.save(vinculo);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("usuarioId", usuarioId);
		payload.put("codigo", item.name());
		payload.put("anterior", anterior);
		payload.put("liberado", liberar);
		auditoriaService.registrar(
			empresa,
			liberar ? "MENU_LIBERADO_USUARIO" : "MENU_REVOGADO_USUARIO",
			"UsuarioAcessoMenu",
			vinculo.getId(),
			payload
		);
		LOGGER.info(
			"Acesso de menu {} {} para usuarioId={} empresaId={}",
			item.name(),
			liberar ? "liberado" : "revogado",
			usuarioId,
			empresaId
		);

		return UsuarioAcessoMenuResponse.de(item, vinculo.isLiberado(), vinculo.getDesde());
	}

	private List<UsuarioAcessoMenuResponse> montarCatalogo(Long usuarioId, boolean todosLiberados) {
		Map<String, UsuarioAcessoMenu> vinculos = usuarioAcessoMenuRepository.findByUsuarioId(usuarioId)
			.stream()
			.collect(Collectors.toMap(
				item -> item.getCodigo().toUpperCase(Locale.ROOT),
				Function.identity(),
				(atual, ignorado) -> atual
			));

		List<UsuarioAcessoMenuResponse> resposta = new ArrayList<>();
		for (AcessoMenu item : AcessoMenu.catalogo()) {
			UsuarioAcessoMenu vinculo = vinculos.get(item.name());
			boolean liberado = todosLiberados || (vinculo != null && vinculo.isLiberado());
			resposta.add(UsuarioAcessoMenuResponse.de(
				item,
				liberado,
				vinculo == null ? null : vinculo.getDesde()
			));
		}
		return resposta;
	}

	private boolean ehAdminEmpresa(UsuarioTokenPrincipal principal) {
		return principal.papeis() != null && principal.papeis().stream()
			.anyMatch(papel -> CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA.equalsIgnoreCase(papel));
	}

	private boolean ehAdminEmpresa(Usuario usuario) {
		return usuarioPapelRepository.findPapeisAtivosByUsuarioId(usuario.getId())
			.stream()
			.anyMatch(vinculo -> CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA
				.equalsIgnoreCase(vinculo.getPapel().getCodigo()));
	}

	private Usuario buscarUsuario(Long empresaId, Long usuarioId) {
		return usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}
}
