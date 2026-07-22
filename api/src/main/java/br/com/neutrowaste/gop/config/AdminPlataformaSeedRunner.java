package br.com.neutrowaste.gop.config;

import br.com.neutrowaste.gop.models.EscopoPapel;
import br.com.neutrowaste.gop.models.Papel;
import br.com.neutrowaste.gop.models.PapelPermissao;
import br.com.neutrowaste.gop.models.Permissao;
import br.com.neutrowaste.gop.models.Usuario;
import br.com.neutrowaste.gop.models.UsuarioPapel;
import br.com.neutrowaste.gop.repositories.PapelPermissaoRepository;
import br.com.neutrowaste.gop.repositories.PapelRepository;
import br.com.neutrowaste.gop.repositories.PermissaoRepository;
import br.com.neutrowaste.gop.repositories.UsuarioPapelRepository;
import br.com.neutrowaste.gop.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@Profile("postgres")
@Order(2)
@EnableConfigurationProperties(AdminPlataformaSeedProperties.class)
public class AdminPlataformaSeedRunner implements ApplicationRunner {

	public static final String CODIGO_PAPEL = "ADMIN_PLATAFORMA";

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminPlataformaSeedRunner.class);

	private static final Map<String, String> PERMISSOES_PLATAFORMA = Map.of(
		"PLATAFORMA_EMPRESA_GERENCIAR", "Gerenciar empresas (tenants) da plataforma",
		"PLATAFORMA_LICENCA_GERENCIAR", "Gerenciar licenças e teto de ativos"
	);

	private final AdminPlataformaSeedProperties properties;
	private final PasswordEncoder passwordEncoder;
	private final PapelRepository papelRepository;
	private final PermissaoRepository permissaoRepository;
	private final PapelPermissaoRepository papelPermissaoRepository;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioPapelRepository usuarioPapelRepository;

	public AdminPlataformaSeedRunner(
		AdminPlataformaSeedProperties properties,
		PasswordEncoder passwordEncoder,
		PapelRepository papelRepository,
		PermissaoRepository permissaoRepository,
		PapelPermissaoRepository papelPermissaoRepository,
		UsuarioRepository usuarioRepository,
		UsuarioPapelRepository usuarioPapelRepository
	) {
		this.properties = properties;
		this.passwordEncoder = passwordEncoder;
		this.papelRepository = papelRepository;
		this.permissaoRepository = permissaoRepository;
		this.papelPermissaoRepository = papelPermissaoRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioPapelRepository = usuarioPapelRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.isEnabled()) {
			LOGGER.info("Seed do admin de plataforma desabilitado");
			return;
		}
		validarConfiguracao();

		Papel papel = garantirPapelAdmin();
		garantirPermissoes(papel);
		Usuario admin = garantirUsuarioAdmin();
		garantirVinculo(admin, papel);

		LOGGER.info(
			"Seed do admin de plataforma concluído: email={}, papel={}",
			admin.getEmail(),
			papel.getCodigo()
		);
	}

	private void validarConfiguracao() {
		if (isBlank(properties.getEmail()) || isBlank(properties.getSenha()) || isBlank(properties.getNome())) {
			throw new IllegalStateException(
				"gop.seed.admin-plataforma exige email, senha e nome preenchidos"
			);
		}
		if (properties.getSenha().length() < 8) {
			throw new IllegalStateException(
				"gop.seed.admin-plataforma.senha deve ter ao menos 8 caracteres"
			);
		}
	}

	private Papel garantirPapelAdmin() {
		return papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull(CODIGO_PAPEL)
			.orElseGet(() -> {
				Papel papel = new Papel();
				papel.setCodigo(CODIGO_PAPEL);
				papel.setNome("Administrador da Plataforma");
				papel.setEscopo(EscopoPapel.PLATAFORMA);
				papel.setAtivo(true);
				papelRepository.save(papel);
				LOGGER.info("Papel {} criado", CODIGO_PAPEL);
				return papel;
			});
	}

	private void garantirPermissoes(Papel papel) {
		Map<String, String> catalogo = new LinkedHashMap<>(PERMISSOES_PLATAFORMA);
		for (Map.Entry<String, String> entrada : catalogo.entrySet()) {
			Permissao permissao = permissaoRepository.findByCodigoIgnoreCase(entrada.getKey())
				.orElseGet(() -> {
					Permissao nova = new Permissao();
					nova.setCodigo(entrada.getKey());
					nova.setDescricao(entrada.getValue());
					nova.setAtivo(true);
					permissaoRepository.save(nova);
					LOGGER.info("Permissão {} criada", entrada.getKey());
					return nova;
				});

			if (!papelPermissaoRepository.existsByPapelIdAndPermissaoId(papel.getId(), permissao.getId())) {
				PapelPermissao vinculo = new PapelPermissao();
				vinculo.setPapel(papel);
				vinculo.setPermissao(permissao);
				papelPermissaoRepository.save(vinculo);
				LOGGER.info("Permissão {} vinculada ao papel {}", permissao.getCodigo(), papel.getCodigo());
			}
		}
	}

	private Usuario garantirUsuarioAdmin() {
		String email = properties.getEmail().strip().toLowerCase(Locale.ROOT);
		return usuarioRepository.findByEmailIgnoreCase(email)
			.orElseGet(() -> {
				Usuario usuario = new Usuario();
				usuario.setEmail(email);
				usuario.setNome(properties.getNome().strip());
				usuario.setSenhaHash(passwordEncoder.encode(properties.getSenha()));
				usuario.setEmpresa(null);
				usuario.setAtivo(true);
				usuarioRepository.save(usuario);
				LOGGER.info("Usuário admin de plataforma criado: {}", email);
				return usuario;
			});
	}

	private void garantirVinculo(Usuario usuario, Papel papel) {
		if (usuarioPapelRepository.existsVinculoPlataforma(usuario.getId(), papel.getId())) {
			return;
		}
		UsuarioPapel vinculo = new UsuarioPapel();
		vinculo.setUsuario(usuario);
		vinculo.setPapel(papel);
		vinculo.setEmpresa(null);
		vinculo.setFilial(null);
		usuarioPapelRepository.save(vinculo);
		LOGGER.info(
			"Papel {} atribuído ao usuário {}",
			papel.getCodigo(),
			usuario.getEmail()
		);
	}

	private boolean isBlank(String valor) {
		return valor == null || valor.isBlank();
	}
}
