package br.com.gop.services;

import br.com.gop.config.CatalogoPlataformaSeedRunner;
import br.com.gop.models.Empresa;
import br.com.gop.models.EscopoPapel;
import br.com.gop.models.OrganizacaoUsuarioRequest;
import br.com.gop.models.OrganizacaoUsuarioResponse;
import br.com.gop.models.Papel;
import br.com.gop.models.Usuario;
import br.com.gop.models.UsuarioPapel;
import br.com.gop.repositories.PapelRepository;
import br.com.gop.repositories.UsuarioPapelRepository;
import br.com.gop.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("postgres")
public class OrganizacaoUsuarioService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrganizacaoUsuarioService.class);

	private final EmpresaService empresaService;
	private final UsuarioRepository usuarioRepository;
	private final PapelRepository papelRepository;
	private final UsuarioPapelRepository usuarioPapelRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditoriaService auditoriaService;

	public OrganizacaoUsuarioService(
		EmpresaService empresaService,
		UsuarioRepository usuarioRepository,
		PapelRepository papelRepository,
		UsuarioPapelRepository usuarioPapelRepository,
		PasswordEncoder passwordEncoder,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.usuarioRepository = usuarioRepository;
		this.papelRepository = papelRepository;
		this.usuarioPapelRepository = usuarioPapelRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<OrganizacaoUsuarioResponse> listar(Long empresaId) {
		empresaService.buscarEntidade(empresaId);
		return usuarioRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public OrganizacaoUsuarioResponse buscar(Long empresaId, Long usuarioId) {
		return paraResponse(buscarUsuarioDaEmpresa(empresaId, usuarioId));
	}

	@Transactional
	public OrganizacaoUsuarioResponse criar(Long empresaId, OrganizacaoUsuarioRequest request) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Não é possível criar usuários para empresa inativa");
		}
		if (request.senha() == null || request.senha().isBlank()) {
			throw new RegraNegocioException("Senha é obrigatória para criar usuário");
		}
		if (request.senha().length() < 8) {
			throw new RegraNegocioException("Senha deve ter ao menos 8 caracteres");
		}

		String email = normalizarEmail(request.email());
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflitoNegocioException("Já existe um usuário com o email informado");
		}

		Usuario usuario = new Usuario();
		usuario.setEmpresa(empresa);
		usuario.setEmail(email);
		usuario.setNome(request.nome().strip());
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario.setAtivo(request.ativo() == null || request.ativo());
		usuarioRepository.save(usuario);

		if (Boolean.TRUE.equals(request.administradorEmpresa())) {
			atribuirAdminEmpresa(usuario, empresa);
		} else {
			atribuirUsuarioEmpresa(usuario, empresa);
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", usuario.getEmail());
		payload.put("nome", usuario.getNome());
		payload.put("administradorEmpresa", Boolean.TRUE.equals(request.administradorEmpresa()));
		auditoriaService.registrar(
			empresa,
			"USUARIO_EMPRESA_CRIADO",
			"Usuario",
			usuario.getId(),
			payload
		);
		LOGGER.info(
			"Usuário da empresa criado: usuarioId={}, empresaId={}, email={}",
			usuario.getId(),
			empresaId,
			email
		);
		return paraResponse(usuario);
	}

	@Transactional
	public OrganizacaoUsuarioResponse atualizar(
		Long empresaId,
		Long usuarioId,
		OrganizacaoUsuarioRequest request
	) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		Usuario usuario = buscarUsuarioDaEmpresa(empresaId, usuarioId);

		String email = normalizarEmail(request.email());
		if (!email.equalsIgnoreCase(usuario.getEmail())
			&& usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflitoNegocioException("Já existe um usuário com o email informado");
		}

		usuario.setEmail(email);
		usuario.setNome(request.nome().strip());
		if (request.ativo() != null) {
			usuario.setAtivo(request.ativo());
		}
		if (request.senha() != null && !request.senha().isBlank()) {
			if (request.senha().length() < 8) {
				throw new RegraNegocioException("Senha deve ter ao menos 8 caracteres");
			}
			usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		}
		usuarioRepository.save(usuario);

		if (request.administradorEmpresa() != null) {
			sincronizarAdminEmpresa(usuario, empresa, request.administradorEmpresa());
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", usuario.getEmail());
		payload.put("nome", usuario.getNome());
		payload.put("ativo", usuario.isAtivo());
		auditoriaService.registrar(
			empresa,
			"USUARIO_EMPRESA_ATUALIZADO",
			"Usuario",
			usuario.getId(),
			payload
		);
		LOGGER.info(
			"Usuário da empresa atualizado: usuarioId={}, empresaId={}",
			usuarioId,
			empresaId
		);
		return paraResponse(usuario);
	}

	@Transactional
	public void desativar(Long empresaId, Long usuarioId, Long solicitanteId) {
		if (usuarioId.equals(solicitanteId)) {
			throw new RegraNegocioException("Não é possível desativar o próprio usuário");
		}
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		Usuario usuario = buscarUsuarioDaEmpresa(empresaId, usuarioId);
		if (!usuario.isAtivo()) {
			return;
		}
		usuario.setAtivo(false);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", usuario.getEmail());
		payload.put("nome", usuario.getNome());
		auditoriaService.registrar(
			empresa,
			"USUARIO_EMPRESA_DESATIVADO",
			"Usuario",
			usuario.getId(),
			payload
		);
		LOGGER.info(
			"Usuário da empresa desativado: usuarioId={}, empresaId={}",
			usuarioId,
			empresaId
		);
	}

	private void sincronizarAdminEmpresa(Usuario usuario, Empresa empresa, boolean tornarAdmin) {
		Papel papelAdmin = papelAdminEmpresa();
		boolean jaEAdmin = usuarioPapelRepository.existsVinculoEmpresa(
			usuario.getId(),
			papelAdmin.getId(),
			empresa.getId()
		);
		if (tornarAdmin && !jaEAdmin) {
			atribuirAdminEmpresa(usuario, empresa);
			removerPapel(usuario, empresa, papelUsuarioEmpresa());
		} else if (!tornarAdmin && jaEAdmin) {
			usuarioPapelRepository.deleteByUsuarioIdAndPapelIdAndEmpresaId(
				usuario.getId(),
				papelAdmin.getId(),
				empresa.getId()
			);
			atribuirUsuarioEmpresa(usuario, empresa);
		} else if (!tornarAdmin) {
			atribuirUsuarioEmpresa(usuario, empresa);
		}
	}

	private void atribuirAdminEmpresa(Usuario usuario, Empresa empresa) {
		atribuirPapel(usuario, empresa, papelAdminEmpresa());
	}

	private void atribuirUsuarioEmpresa(Usuario usuario, Empresa empresa) {
		atribuirPapel(usuario, empresa, papelUsuarioEmpresa());
	}

	private void atribuirPapel(Usuario usuario, Empresa empresa, Papel papel) {
		if (usuarioPapelRepository.existsVinculoEmpresa(
			usuario.getId(),
			papel.getId(),
			empresa.getId()
		)) {
			return;
		}
		UsuarioPapel vinculo = new UsuarioPapel();
		vinculo.setUsuario(usuario);
		vinculo.setPapel(papel);
		vinculo.setEmpresa(empresa);
		vinculo.setFilial(null);
		usuarioPapelRepository.save(vinculo);
	}

	private void removerPapel(Usuario usuario, Empresa empresa, Papel papel) {
		if (!usuarioPapelRepository.existsVinculoEmpresa(
			usuario.getId(),
			papel.getId(),
			empresa.getId()
		)) {
			return;
		}
		usuarioPapelRepository.deleteByUsuarioIdAndPapelIdAndEmpresaId(
			usuario.getId(),
			papel.getId(),
			empresa.getId()
		);
	}

	private Papel papelAdminEmpresa() {
		return papelAtivo(
			CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA,
			EscopoPapel.ORGANIZACAO
		);
	}

	private Papel papelUsuarioEmpresa() {
		return papelAtivo(
			CatalogoPlataformaSeedRunner.CODIGO_PAPEL_USUARIO_EMPRESA,
			EscopoPapel.OPERACIONAL
		);
	}

	private Papel papelAtivo(String codigo, EscopoPapel escopoEsperado) {
		Papel papel = papelRepository
			.findByCodigoIgnoreCaseAndEmpresaIsNull(codigo)
			.orElseThrow(() -> new RegraNegocioException(
				"Papel " + codigo + " não configurado na plataforma"
			));
		if (papel.getEscopo() != escopoEsperado || !papel.isAtivo()) {
			throw new RegraNegocioException("Papel " + codigo + " inválido ou inativo");
		}
		return papel;
	}

	private Usuario buscarUsuarioDaEmpresa(Long empresaId, Long usuarioId) {
		return usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

	private OrganizacaoUsuarioResponse paraResponse(Usuario usuario) {
		List<String> papeis = usuarioPapelRepository.findPapeisAtivosByUsuarioId(usuario.getId())
			.stream()
			.map(vinculo -> vinculo.getPapel().getCodigo().toUpperCase(Locale.ROOT))
			.distinct()
			.toList();
		boolean admin = papeis.stream()
			.anyMatch(codigo -> CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA.equalsIgnoreCase(codigo));
		return new OrganizacaoUsuarioResponse(
			usuario.getId(),
			usuario.getEmpresa().getId(),
			usuario.getEmail(),
			usuario.getNome(),
			usuario.isAtivo(),
			admin,
			papeis,
			usuario.getCriadoEm(),
			usuario.getAtualizadoEm()
		);
	}

	private String normalizarEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}
