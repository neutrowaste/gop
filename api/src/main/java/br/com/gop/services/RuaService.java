package br.com.gop.services;

import br.com.gop.models.Cidade;
import br.com.gop.models.Empresa;
import br.com.gop.models.Rua;
import br.com.gop.models.RuaRequest;
import br.com.gop.models.RuaResponse;
import br.com.gop.repositories.RuaRepository;
import org.locationtech.jts.geom.LineString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("postgres")
public class RuaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RuaService.class);

	private final EmpresaService empresaService;
	private final CidadeService cidadeService;
	private final RuaRepository ruaRepository;
	private final AuditoriaService auditoriaService;

	public RuaService(
		EmpresaService empresaService,
		CidadeService cidadeService,
		RuaRepository ruaRepository,
		AuditoriaService auditoriaService
	) {
		this.empresaService = empresaService;
		this.cidadeService = cidadeService;
		this.ruaRepository = ruaRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional(readOnly = true)
	public List<RuaResponse> listar(Long empresaId, Long cidadeId) {
		empresaService.buscarEntidade(empresaId);
		List<Rua> ruas = cidadeId == null
			? ruaRepository.findByEmpresaIdOrderByNomeAsc(empresaId)
			: ruaRepository.findByEmpresaIdAndCidadeIdOrderByNomeAsc(empresaId, cidadeId);
		return ruas.stream().map(this::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public RuaResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public RuaResponse criar(Long empresaId, RuaRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Cidade cidade = cidadeService.buscarEntidade(empresaId, request.cidadeId());
		LineString geom = GeometriaWkt.lineStringDeWkt(request.geomWkt());

		Rua rua = new Rua();
		rua.setEmpresa(empresa);
		rua.setCidade(cidade);
		rua.setNome(request.nome().strip());
		rua.setGeom(geom);
		rua.setAtivo(request.ativo() == null || request.ativo());
		ruaRepository.save(rua);

		auditoriaService.registrar(
			empresa,
			"RUA_CRIADA",
			"Rua",
			rua.getId(),
			Map.of("nome", rua.getNome(), "cidadeId", cidade.getId())
		);
		LOGGER.info("Rua criada: id={}, empresaId={}, nome={}", rua.getId(), empresaId, rua.getNome());
		return paraResponse(rua);
	}

	@Transactional
	public RuaResponse atualizar(Long empresaId, Long id, RuaRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Rua rua = buscarEntidade(empresaId, id);
		Cidade cidade = cidadeService.buscarEntidade(empresaId, request.cidadeId());
		LineString geom = GeometriaWkt.lineStringDeWkt(request.geomWkt());

		rua.setCidade(cidade);
		rua.setNome(request.nome().strip());
		rua.setGeom(geom);
		if (request.ativo() != null) {
			rua.setAtivo(request.ativo());
		}
		ruaRepository.save(rua);

		auditoriaService.registrar(
			empresa,
			"RUA_ATUALIZADA",
			"Rua",
			rua.getId(),
			new LinkedHashMap<>(Map.of("nome", rua.getNome(), "cidadeId", cidade.getId()))
		);
		LOGGER.info("Rua atualizada: id={}, empresaId={}", id, empresaId);
		return paraResponse(rua);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Rua rua = buscarEntidade(empresaId, id);
		rua.setAtivo(false);
		ruaRepository.save(rua);
		auditoriaService.registrar(empresa, "RUA_DESATIVADA", "Rua", id, Map.of("nome", rua.getNome()));
		LOGGER.info("Rua desativada: id={}, empresaId={}", id, empresaId);
	}

	@Transactional(readOnly = true)
	public Rua buscarEntidade(Long empresaId, Long id) {
		return ruaRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Rua não encontrada"));
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private RuaResponse paraResponse(Rua rua) {
		return new RuaResponse(
			rua.getId(),
			rua.getCidade().getId(),
			rua.getCidade().getNome(),
			rua.getNome(),
			GeometriaWkt.paraWkt(rua.getGeom()),
			rua.isAtivo()
		);
	}
}
