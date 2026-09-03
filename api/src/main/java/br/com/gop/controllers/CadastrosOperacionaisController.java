package br.com.gop.controllers;

import br.com.gop.models.CidadeRequest;
import br.com.gop.models.CidadeResponse;
import br.com.gop.models.ColaboradorRequest;
import br.com.gop.models.ColaboradorResponse;
import br.com.gop.models.EquipamentoRequest;
import br.com.gop.models.EquipamentoResponse;
import br.com.gop.models.SetorDemarcacaoPreviewResponse;
import br.com.gop.models.SetorDemarcacaoRequest;
import br.com.gop.models.SetorRequest;
import br.com.gop.models.SetorResponse;
import br.com.gop.models.TrechoDesenhoRequest;
import br.com.gop.models.TrechoImportacaoResponse;
import br.com.gop.models.TrechoRequest;
import br.com.gop.models.TrechoResponse;
import br.com.gop.security.OrgAuth;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.CidadeService;
import br.com.gop.services.ColaboradorService;
import br.com.gop.services.EquipamentoService;
import br.com.gop.services.SetorService;
import br.com.gop.services.TrechoService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Cadastros operacionais da empresa (setor, trecho, colaborador, lutocar).
 * Importação de shapefile: ZIP com .shp/.shx/.dbf, atributo RUA no feature.
 */
@RestController
@RequestMapping("/v1/org")
@Profile("postgres")
@PreAuthorize("isAuthenticated()")
public class CadastrosOperacionaisController {

	private final CidadeService cidadeService;
	private final SetorService setorService;
	private final TrechoService trechoService;
	private final ColaboradorService colaboradorService;
	private final EquipamentoService equipamentoService;

	public CadastrosOperacionaisController(
		CidadeService cidadeService,
		SetorService setorService,
		TrechoService trechoService,
		ColaboradorService colaboradorService,
		EquipamentoService equipamentoService
	) {
		this.cidadeService = cidadeService;
		this.setorService = setorService;
		this.trechoService = trechoService;
		this.colaboradorService = colaboradorService;
		this.equipamentoService = equipamentoService;
	}

	@GetMapping("/cidades")
	@PreAuthorize(OrgAuth.LER_CIDADES)
	public List<CidadeResponse> listarCidades() {
		return cidadeService.listar(empresaId());
	}

	@GetMapping("/cidades/{id}")
	@PreAuthorize(OrgAuth.LER_CIDADES)
	public CidadeResponse buscarCidade(@PathVariable Long id) {
		return cidadeService.buscar(empresaId(), id);
	}

	@PostMapping("/cidades")
	@PreAuthorize(OrgAuth.SETORES)
	public ResponseEntity<CidadeResponse> criarCidade(@Valid @RequestBody CidadeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(cidadeService.criar(empresaId(), request));
	}

	@PutMapping("/cidades/{id}")
	@PreAuthorize(OrgAuth.SETORES)
	public CidadeResponse atualizarCidade(@PathVariable Long id, @Valid @RequestBody CidadeRequest request) {
		return cidadeService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/cidades/{id}")
	@PreAuthorize(OrgAuth.SETORES)
	public ResponseEntity<Void> desativarCidade(@PathVariable Long id) {
		cidadeService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/setores")
	@PreAuthorize(OrgAuth.LER_SETORES)
	public List<SetorResponse> listarSetores(
		@RequestParam(required = false) Long cidadeId,
		@RequestParam(required = false) Long moduloId
	) {
		return setorService.listar(empresaId(), cidadeId, moduloId);
	}

	@GetMapping("/setores/{id}")
	@PreAuthorize(OrgAuth.LER_SETORES)
	public SetorResponse buscarSetor(@PathVariable Long id) {
		return setorService.buscar(empresaId(), id);
	}

	@PostMapping("/setores")
	@PreAuthorize(OrgAuth.SETORES)
	public ResponseEntity<SetorResponse> criarSetor(@Valid @RequestBody SetorRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(setorService.criar(empresaId(), request));
	}

	@PutMapping("/setores/{id}")
	@PreAuthorize(OrgAuth.SETORES)
	public SetorResponse atualizarSetor(@PathVariable Long id, @Valid @RequestBody SetorRequest request) {
		return setorService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/setores/{id}")
	@PreAuthorize(OrgAuth.SETORES)
	public ResponseEntity<Void> desativarSetor(@PathVariable Long id) {
		setorService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(value = "/setores/shapefile-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize(OrgAuth.SETORES)
	public SetorDemarcacaoPreviewResponse preVisualizarShapefileSetor(
		@RequestParam("arquivo") MultipartFile arquivo,
		@RequestParam(required = false) String charset
	) {
		return setorService.preVisualizarShapefile(empresaId(), arquivo, charset);
	}

	@PostMapping(value = "/setores/{id}/shapefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize(OrgAuth.SETORES)
	public SetorResponse importarShapefileSetor(
		@PathVariable Long id,
		@RequestParam("arquivo") MultipartFile arquivo,
		@RequestParam(required = false) String charset
	) {
		return setorService.importarShapefile(empresaId(), id, arquivo, charset);
	}

	@PutMapping("/setores/{id}/demarcacao")
	@PreAuthorize(OrgAuth.SETORES)
	public SetorResponse atualizarDemarcacaoSetor(
		@PathVariable Long id,
		@Valid @RequestBody SetorDemarcacaoRequest request
	) {
		return setorService.atualizarDemarcacao(empresaId(), id, request.geomWkt());
	}

	@DeleteMapping("/setores/{id}/shapefile")
	@PreAuthorize(OrgAuth.SETORES)
	public SetorResponse removerShapefileSetor(@PathVariable Long id) {
		return setorService.removerDemarcacao(empresaId(), id);
	}

	@GetMapping("/trechos")
	@PreAuthorize(OrgAuth.LER_TRECHOS)
	public List<TrechoResponse> listarTrechos(@RequestParam(required = false) Long setorId) {
		return trechoService.listar(empresaId(), setorId);
	}

	@GetMapping("/trechos/{id}")
	@PreAuthorize(OrgAuth.LER_TRECHOS)
	public TrechoResponse buscarTrecho(@PathVariable Long id) {
		return trechoService.buscar(empresaId(), id);
	}

	@PostMapping("/trechos")
	@PreAuthorize(OrgAuth.TRECHOS)
	public ResponseEntity<TrechoResponse> criarTrecho(@Valid @RequestBody TrechoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(trechoService.criar(empresaId(), request));
	}

	@PostMapping("/trechos/desenho")
	@PreAuthorize(OrgAuth.TRECHOS)
	public TrechoImportacaoResponse criarTrechosDesenho(@Valid @RequestBody TrechoDesenhoRequest request) {
		return trechoService.criarDesenho(empresaId(), request);
	}

	@PostMapping(value = "/trechos/importar-shapefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize(OrgAuth.TRECHOS)
	public TrechoImportacaoResponse importarShapefile(
		@RequestParam Long setorId,
		@RequestParam("arquivo") MultipartFile arquivo,
		@RequestParam(required = false) String atributoRua,
		@RequestParam(required = false) String atributoCodigo,
		@RequestParam(required = false) String charset
	) {
		return trechoService.importarShapefile(empresaId(), setorId, arquivo, atributoRua, atributoCodigo, charset);
	}

	@PutMapping("/trechos/{id}")
	@PreAuthorize(OrgAuth.TRECHOS)
	public TrechoResponse atualizarTrecho(@PathVariable Long id, @Valid @RequestBody TrechoRequest request) {
		return trechoService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/trechos/{id}")
	@PreAuthorize(OrgAuth.TRECHOS)
	public ResponseEntity<Void> desativarTrecho(@PathVariable Long id) {
		trechoService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/colaboradores")
	@PreAuthorize(OrgAuth.LER_COLABORADORES)
	public List<ColaboradorResponse> listarColaboradores() {
		return colaboradorService.listar(empresaId());
	}

	@GetMapping("/colaboradores/{id}")
	@PreAuthorize(OrgAuth.LER_COLABORADORES)
	public ColaboradorResponse buscarColaborador(@PathVariable Long id) {
		return colaboradorService.buscar(empresaId(), id);
	}

	@PostMapping("/colaboradores")
	@PreAuthorize(OrgAuth.COLABORADORES)
	public ResponseEntity<ColaboradorResponse> criarColaborador(
		@Valid @RequestBody ColaboradorRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(colaboradorService.criar(empresaId(), request));
	}

	@PutMapping("/colaboradores/{id}")
	@PreAuthorize(OrgAuth.COLABORADORES)
	public ColaboradorResponse atualizarColaborador(
		@PathVariable Long id,
		@Valid @RequestBody ColaboradorRequest request
	) {
		return colaboradorService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/colaboradores/{id}")
	@PreAuthorize(OrgAuth.COLABORADORES)
	public ResponseEntity<Void> desativarColaborador(@PathVariable Long id) {
		colaboradorService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/equipamentos")
	@PreAuthorize(OrgAuth.LER_COLABORADORES)
	public List<EquipamentoResponse> listarEquipamentos() {
		return equipamentoService.listar(empresaId());
	}

	@GetMapping("/equipamentos/{id}")
	@PreAuthorize(OrgAuth.LER_COLABORADORES)
	public EquipamentoResponse buscarEquipamento(@PathVariable Long id) {
		return equipamentoService.buscar(empresaId(), id);
	}

	@PostMapping("/equipamentos")
	@PreAuthorize(OrgAuth.COLABORADORES)
	public ResponseEntity<EquipamentoResponse> criarEquipamento(
		@Valid @RequestBody EquipamentoRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(equipamentoService.criar(empresaId(), request));
	}

	@PutMapping("/equipamentos/{id}")
	@PreAuthorize(OrgAuth.COLABORADORES)
	public EquipamentoResponse atualizarEquipamento(
		@PathVariable Long id,
		@Valid @RequestBody EquipamentoRequest request
	) {
		return equipamentoService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/equipamentos/{id}")
	@PreAuthorize(OrgAuth.COLABORADORES)
	public ResponseEntity<Void> desativarEquipamento(@PathVariable Long id) {
		equipamentoService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
