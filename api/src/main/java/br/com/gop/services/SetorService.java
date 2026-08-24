package br.com.gop.services;

import br.com.gop.models.Cidade;
import br.com.gop.models.Empresa;
import br.com.gop.models.Modulo;
import br.com.gop.models.Setor;
import br.com.gop.models.SetorDemarcacaoPreviewResponse;
import br.com.gop.models.SetorRequest;
import br.com.gop.models.SetorResponse;
import br.com.gop.repositories.EmpresaModuloRepository;
import br.com.gop.repositories.ModuloRepository;
import br.com.gop.repositories.SetorRepository;
import org.locationtech.jts.geom.MultiPolygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("postgres")
public class SetorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetorService.class);

	private final EmpresaService empresaService;
	private final CidadeService cidadeService;
	private final ModuloRepository moduloRepository;
	private final EmpresaModuloRepository empresaModuloRepository;
	private final SetorRepository setorRepository;
	private final AuditoriaService auditoriaService;

	public SetorService(
		EmpresaService empresaService,
		CidadeService cidadeService,
		ModuloRepository moduloRepository,
		EmpresaModuloRepository empresaModuloRepository,
		SetorRepository setorRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.cidadeService = cidadeService;
		this.moduloRepository = moduloRepository;
		this.empresaModuloRepository = empresaModuloRepository;
		this.setorRepository = setorRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<SetorResponse> listar(Long empresaId, Long cidadeId, Long moduloId) {
		empresaService.buscarEntidade(empresaId);
		return setorRepository.findByEmpresaFiltrado(empresaId, cidadeId, moduloId)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public SetorResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public SetorResponse criar(Long empresaId, SetorRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Modulo modulo = moduloContratado(empresaId, request.moduloId());
		Cidade cidade = cidadeService.buscarEntidade(empresaId, request.cidadeId());
		String codigo = normalizarCodigo(request.codigo());
		if (codigo != null) {
			garantirCodigoUnicoNoServico(empresaId, modulo.getId(), codigo, null);
		}

		Setor setor = new Setor();
		setor.setEmpresa(empresa);
		setor.setModulo(modulo);
		setor.setCidade(cidade);
		setor.setCodigo(codigo);
		setor.setNome(request.nome().strip());
		setor.setAtivo(request.ativo() == null || request.ativo());
		setorRepository.save(setor);

		auditoriaService.registrar(
			empresa,
			"SETOR_CRIADO",
			"Setor",
			setor.getId(),
			Map.of(
				"nome", setor.getNome(),
				"codigo", codigo == null ? "" : codigo,
				"moduloId", modulo.getId(),
				"cidadeId", cidade.getId()
			)
		);
		LOGGER.info(
			"Setor criado: id={}, empresaId={}, modulo={}, codigo={}",
			setor.getId(),
			empresaId,
			modulo.getCodigo(),
			codigo
		);
		return paraResponse(setor);
	}

	@Transactional
	public SetorResponse atualizar(Long empresaId, Long id, SetorRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = buscarEntidade(empresaId, id);
		Modulo modulo = moduloContratado(empresaId, request.moduloId());
		Cidade cidade = cidadeService.buscarEntidade(empresaId, request.cidadeId());
		String codigo = normalizarCodigo(request.codigo());
		if (codigo != null) {
			garantirCodigoUnicoNoServico(empresaId, modulo.getId(), codigo, id);
		}

		setor.setModulo(modulo);
		setor.setCidade(cidade);
		setor.setCodigo(codigo);
		setor.setNome(request.nome().strip());
		if (request.ativo() != null) {
			setor.setAtivo(request.ativo());
		}
		setorRepository.save(setor);

		auditoriaService.registrar(
			empresa,
			"SETOR_ATUALIZADO",
			"Setor",
			setor.getId(),
			new LinkedHashMap<>(Map.of(
				"nome", setor.getNome(),
				"moduloId", modulo.getId(),
				"cidadeId", cidade.getId()
			))
		);
		LOGGER.info("Setor atualizado: id={}, empresaId={}, modulo={}", id, empresaId, modulo.getCodigo());
		return paraResponse(setor);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = buscarEntidade(empresaId, id);
		setor.setAtivo(false);
		setorRepository.save(setor);
		auditoriaService.registrar(empresa, "SETOR_DESATIVADO", "Setor", id, Map.of("nome", setor.getNome()));
		LOGGER.info("Setor desativado: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public SetorDemarcacaoPreviewResponse preVisualizarShapefile(Long empresaId, MultipartFile arquivo, String charset) {
		empresaService.buscarEntidade(empresaId);
		MultiPolygon geom = lerShapefileZip(arquivo, charset);
		return new SetorDemarcacaoPreviewResponse(GeometriaWkt.paraWkt(geom), GeometriaWkt.paraGeoJson(geom));
	}

	@Transactional
	public SetorResponse importarShapefile(Long empresaId, Long id, MultipartFile arquivo, String charset) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = buscarEntidade(empresaId, id);
		if (!setor.isAtivo()) {
			throw new RegraNegocioException("Setor informado está inativo");
		}
		setor.setGeom(lerShapefileZip(arquivo, charset));
		setorRepository.save(setor);

		auditoriaService.registrar(
			empresa,
			"SETOR_DEMARCACAO_IMPORTADA",
			"Setor",
			setor.getId(),
			Map.of("nome", setor.getNome())
		);
		LOGGER.info("Demarcação importada: setorId={}, empresaId={}", id, empresaId);
		return paraResponse(setor);
	}

	@Transactional
	public SetorResponse atualizarDemarcacao(Long empresaId, Long id, String geomWkt) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = buscarEntidade(empresaId, id);
		if (!setor.isAtivo()) {
			throw new RegraNegocioException("Setor informado está inativo");
		}
		setor.setGeom(GeometriaWkt.multiPolygonDeWkt(geomWkt));
		setorRepository.save(setor);
		auditoriaService.registrar(
			empresa,
			"SETOR_DEMARCACAO_ATUALIZADA",
			"Setor",
			setor.getId(),
			Map.of("nome", setor.getNome())
		);
		LOGGER.info("Demarcação atualizada: setorId={}, empresaId={}", id, empresaId);
		return paraResponse(setor);
	}

	@Transactional
	public SetorResponse removerDemarcacao(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = buscarEntidade(empresaId, id);
		setor.setGeom(null);
		setorRepository.save(setor);
		auditoriaService.registrar(
			empresa,
			"SETOR_DEMARCACAO_REMOVIDA",
			"Setor",
			id,
			Map.of("nome", setor.getNome())
		);
		LOGGER.info("Demarcação removida: setorId={}, empresaId={}", id, empresaId);
		return paraResponse(setor);
	}

	@Transactional(readOnly = true)
	public Setor buscarEntidade(Long empresaId, Long id) {
		return setorRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado"));
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private Modulo moduloContratado(Long empresaId, Long moduloId) {
		Modulo modulo = moduloRepository.findById(moduloId)
			.filter(Modulo::isAtivo)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
		if (!empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(empresaId).contains(modulo.getId())) {
			throw new RegraNegocioException("O setor deve estar vinculado a um serviço contratado pela empresa");
		}
		return modulo;
	}

	private void garantirCodigoUnicoNoServico(Long empresaId, Long moduloId, String codigo, Long idAtual) {
		boolean existe = idAtual == null
			? setorRepository.existsByEmpresaIdAndModuloIdAndCodigoIgnoreCase(empresaId, moduloId, codigo)
			: setorRepository.existsByEmpresaIdAndModuloIdAndCodigoIgnoreCaseAndIdNot(
				empresaId,
				moduloId,
				codigo,
				idAtual
			);
		if (existe) {
			throw new ConflitoNegocioException("Já existe setor com este código neste serviço");
		}
	}

	private String normalizarCodigo(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return null;
		}
		return codigo.strip().toUpperCase(Locale.ROOT);
	}

	private SetorResponse paraResponse(Setor setor) {
		Modulo modulo = setor.getModulo();
		return new SetorResponse(
			setor.getId(),
			modulo.getId(),
			modulo.getCodigo(),
			modulo.getNome(),
			setor.getCidade().getId(),
			setor.getCidade().getNome(),
			setor.getCodigo(),
			setor.getNome(),
			setor.getGeom() != null && !setor.getGeom().isEmpty(),
			GeometriaWkt.paraWkt(setor.getGeom()),
			GeometriaWkt.paraGeoJson(setor.getGeom()),
			setor.isAtivo()
		);
	}

	private MultiPolygon lerShapefileZip(MultipartFile arquivo, String charset) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new RegraNegocioException("Envie um ZIP contendo o shapefile (.shp, .shx, .dbf)");
		}
		String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
		if (!nome.endsWith(".zip")) {
			throw new RegraNegocioException("Envie um arquivo .zip com o shapefile de área");
		}

		Path temp = null;
		try {
			temp = Files.createTempDirectory("gop-setor-shp-");
			Path shp;
			try (var in = arquivo.getInputStream()) {
				shp = ShapefileZip.extrair(in, temp);
			}
			return ShapefileSetorParser.ler(shp, charset);
		} catch (IOException exception) {
			throw new RegraNegocioException("Falha ao processar o ZIP do shapefile");
		} finally {
			apagarDiretorio(temp);
		}
	}

	private static void apagarDiretorio(Path diretorio) {
		if (diretorio == null || !Files.exists(diretorio)) {
			return;
		}
		try (var stream = Files.walk(diretorio)) {
			stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
					// temp
				}
			});
		} catch (IOException ignored) {
			// temp
		}
	}
}
