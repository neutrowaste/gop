package br.com.neutrowaste.gop.config;

import br.com.neutrowaste.gop.models.CategoriaModulo;
import br.com.neutrowaste.gop.models.EscopoPapel;
import br.com.neutrowaste.gop.models.Modulo;
import br.com.neutrowaste.gop.models.Papel;
import br.com.neutrowaste.gop.repositories.ModuloRepository;
import br.com.neutrowaste.gop.repositories.PapelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("postgres")
@Order(1)
public class CatalogoPlataformaSeedRunner implements ApplicationRunner {

	public static final String CODIGO_PAPEL_ADMIN_EMPRESA = "ADMIN_EMPRESA";

	private static final Logger LOGGER = LoggerFactory.getLogger(CatalogoPlataformaSeedRunner.class);

	private static final List<ModuloSeed> MODULOS = List.of(
		new ModuloSeed("ESCALA", "Escala Diária", CategoriaModulo.OPERACIONAL, 10),
		new ModuloSeed("VARRIACAO_VIAS", "Varrição de Vias", CategoriaModulo.OPERACIONAL, 20),
		new ModuloSeed("COLETA_PAPELEIRAS", "Coleta de Papeleiras", CategoriaModulo.OPERACIONAL, 30),
		new ModuloSeed("MONITORAMENTO", "Monitoramento e Mapa", CategoriaModulo.TRANSVERSAL, 40),
		new ModuloSeed("AUDITORIA", "Auditoria e Informe", CategoriaModulo.SUPORTE, 50)
	);

	private final ModuloRepository moduloRepository;
	private final PapelRepository papelRepository;

	public CatalogoPlataformaSeedRunner(
		ModuloRepository moduloRepository,
		PapelRepository papelRepository
	) {
		this.moduloRepository = moduloRepository;
		this.papelRepository = papelRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		garantirModulos();
		garantirPapelAdminEmpresa();
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

	private void garantirPapelAdminEmpresa() {
		papelRepository.findByCodigoIgnoreCaseAndEmpresaIsNull(CODIGO_PAPEL_ADMIN_EMPRESA)
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

	private record ModuloSeed(String codigo, String nome, CategoriaModulo categoria, int ordem) {
	}
}
