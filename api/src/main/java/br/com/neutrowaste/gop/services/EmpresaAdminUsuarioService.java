package br.com.neutrowaste.gop.services;

import br.com.neutrowaste.gop.config.CatalogoPlataformaSeedRunner;
import br.com.neutrowaste.gop.models.Empresa;
import br.com.neutrowaste.gop.models.EmpresaAdminRequest;
import br.com.neutrowaste.gop.models.EmpresaAdminResponse;
import br.com.neutrowaste.gop.models.EscopoPapel;
import br.com.neutrowaste.gop.models.Papel;
import br.com.neutrowaste.gop.models.Usuario;
import br.com.neutrowaste.gop.models.UsuarioPapel;
import br.com.neutrowaste.gop.repositories.PapelRepository;
import br.com.neutrowaste.gop.repositories.UsuarioPapelRepository;
import br.com.neutrowaste.gop.repositories.UsuarioRepository;
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
public class EmpresaAdminUsuarioService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmpresaAdminUsuarioService.class);

	private final EmpresaService empresaService;
	private final UsuarioRepository usuarioRepository;
	private final PapelRepository papelRepository;
	private final UsuarioPapelRepository usuarioPapelRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditoriaService auditoriaService;

	public EmpresaAdminUsuarioService(
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
	public List<EmpresaAdminResponse> listar(Long empresaId) {
		empresaService.buscarEntidade(empresaId);
		return usuarioRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional
	public EmpresaAdminResponse criar(Long empresaId, EmpresaAdminRequest request) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Não é possível criar admin para empresa inativa");
		}

		String email = request.email().strip().toLowerCase(Locale.ROOT);
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflitoNegocioException("Já existe um usuário com o email informado");
		}

		Papel papel = papelRepository
			.findByCodigoIgnoreCaseAndEmpresaIsNull(CatalogoPlataformaSeedRunner.CODIGO_PAPEL_ADMIN_EMPRESA)
			.orElseThrow(() -> new RegraNegocioException(
				"Papel ADMIN_EMPRESA não configurado na plataforma"
			));
		if (papel.getEscopo() != EscopoPapel.ORGANIZACAO || !papel.isAtivo()) {
			throw new RegraNegocioException("Papel ADMIN_EMPRESA inválido ou inativo");
		}

		Usuario usuario = new Usuario();
		usuario.setEmpresa(empresa);
		usuario.setEmail(email);
		usuario.setNome(request.nome().strip());
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario.setAtivo(true);
		usuarioRepository.save(usuario);

		UsuarioPapel vinculo = new UsuarioPapel();
		vinculo.setUsuario(usuario);
		vinculo.setPapel(papel);
		vinculo.setEmpresa(empresa);
		vinculo.setFilial(null);
		usuarioPapelRepository.save(vinculo);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", usuario.getEmail());
		payload.put("nome", usuario.getNome());
		payload.put("papel", papel.getCodigo());
		auditoriaService.registrar(
			empresa,
			"ADMIN_EMPRESA_CRIADO",
			"Usuario",
			usuario.getId(),
			payload
		);
		LOGGER.info(
			"Admin da empresa criado: usuarioId={}, empresaId={}, email={}",
			usuario.getId(),
			empresaId,
			email
		);
		return paraResponse(usuario);
	}

	@Transactional
	public void desativar(Long empresaId, Long usuarioId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		Usuario usuario = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Admin da empresa não encontrado"));

		if (!usuario.isAtivo()) {
			return;
		}

		usuario.setAtivo(false);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", usuario.getEmail());
		payload.put("nome", usuario.getNome());
		auditoriaService.registrar(
			empresa,
			"ADMIN_EMPRESA_DESATIVADO",
			"Usuario",
			usuario.getId(),
			payload
		);
		LOGGER.info(
			"Admin da empresa desativado: usuarioId={}, empresaId={}",
			usuarioId,
			empresaId
		);
	}

	private EmpresaAdminResponse paraResponse(Usuario usuario) {
		return new EmpresaAdminResponse(
			usuario.getId(),
			usuario.getEmpresa().getId(),
			usuario.getEmail(),
			usuario.getNome(),
			usuario.isAtivo(),
			usuario.getCriadoEm(),
			usuario.getAtualizadoEm()
		);
	}
}
