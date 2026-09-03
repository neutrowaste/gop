package br.com.gop.config;

import br.com.gop.models.CategoriaModulo;
import br.com.gop.models.EscopoPapel;
import br.com.gop.models.Modulo;
import br.com.gop.models.Papel;
import br.com.gop.models.PapelPermissao;
import br.com.gop.models.Permissao;
import br.com.gop.repositories.ModuloRepository;
import br.com.gop.repositories.PapelPermissaoRepository;
import br.com.gop.repositories.PapelRepository;
import br.com.gop.repositories.PermissaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("postgres")
@Order(1)
public class CatalogoPlataformaSeedRunner implements ApplicationRunner {

	public static final String CODIGO_PAPEL_ADMIN_EMPRESA = "ADMIN_EMPRESA";
	public static final String CODIGO_PAPEL_USUARIO_EMPRESA = "USUARIO_EMPRESA";

	private static final Logger LOGGER = LoggerFactory.getLogger(CatalogoPlataformaSeedRunner.class);

	private static final List<ModuloSeed> MODULOS = List.of(
		new ModuloSeed("ESCALA", "Escala Diária", CategoriaModulo.OPERACIONAL, 10),
		new ModuloSeed("VARRIACAO_VIAS", "Varrição de Vias", CategoriaModulo.OPERACIONAL, 20),
		new ModuloSeed("COLETA_PAPELEIRAS", "Coleta de Papeleiras", CategoriaModulo.OPERACIONAL, 30),
		new ModuloSeed("MONITORAMENTO", "Monitoramento e Mapa", CategoriaModulo.TRANSVERSAL, 40),
		new ModuloSeed("AUDITORIA", "Auditoria e Informe", CategoriaModulo.SUPORTE, 50)
	);

	private static final Map<String, String> PERMISSOES_ADMIN_EMPRESA = Map.of(
		"EMPRESA_USUARIO_GERENCIAR", "Gerenciar usuários da própria empresa",
		"EMPRESA_MODULO_LIBERAR", "Liberar módulos contratados aos usuários da empresa"
	);

	private final ModuloRepository moduloRepository;
	private final PapelRepository papelRepository;
	private final PermissaoRepository permissaoRepository;
	private final PapelPermissaoRepository papelPermissaoRepository;

	public CatalogoPlataformaSeedRunner(
		ModuloRepository moduloRepository,
		PapelRepository papelRepository,
		PermissaoRepository permissaoRepository,
		PapelPermissaoRepository papelPermissaoRepository
	) {
		this.moduloRepository = moduloRepository;
		this.papelRepository = papelRepository;
		this.permissaoRepository = permissaoRepository;
		this.papelPermissaoRepository = papelPermissaoRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		garantirModulos();
		Papel papel = garantirPapelAdminEmpresa();
		garantirPermissoesAdminEmpresa(papel);
		garantirPapelUsuarioEmpresa();
	}

	private void garantirModulos() {
		for (ModuloSeed seed : MODULOS) {
			moduloRepository.findByCodigoIgnoreCase(seed.codigo())
				.orElseGet(() -> {
					Modulo modulo = new Modulo();
					modulo.setCodigo(seed.codigo());
					modulo.setNome(seed.nome());
					modulo.setCategoria(seed.categoria());
					modulo.setOrdem(seed.ordem());
					modulo.setAtivo(true);
					moduloRepository.save(modulo);
					LOGGER.info("Módulo {} criado", seed.codigo());
					return modulo;
				});
		}
	}

	private Papel garantirPapelUsuarioEmpresa() {
		return papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull(CODIGO_PAPEL_USUARIO_EMPRESA)
			.orElseGet(() -> {
				Papel papel = new Papel();
				papel.setCodigo(CODIGO_PAPEL_USUARIO_EMPRESA);
				papel.setNome("Usuário da Empresa");
				papel.setEscopo(EscopoPapel.OPERACIONAL);
				papel.setAtivo(true);
				papelRepository.save(papel);
				LOGGER.info("Papel {} criado", CODIGO_PAPEL_USUARIO_EMPRESA);
				return papel;
			});
	}

	private Papel garantirPapelAdminEmpresa() {
		return papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull(CODIGO_PAPEL_ADMIN_EMPRESA)
			.orElseGet(() -> {
				Papel papel = new Papel();
				papel.setCodigo(CODIGO_PAPEL_ADMIN_EMPRESA);
				papel.setNome("Administrador da Empresa");
				papel.setEscopo(EscopoPapel.ORGANIZACAO);
				papel.setAtivo(true);
				papelRepository.save(papel);
				LOGGER.info("Papel {} criado", CODIGO_PAPEL_ADMIN_EMPRESA);
				return papel;
			});
	}

	private void garantirPermissoesAdminEmpresa(Papel papel) {
		Map<String, String> catalogo = new LinkedHashMap<>(PERMISSOES_ADMIN_EMPRESA);
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

	private record ModuloSeed(String codigo, String nome, CategoriaModulo categoria, int ordem) {
	}
}
