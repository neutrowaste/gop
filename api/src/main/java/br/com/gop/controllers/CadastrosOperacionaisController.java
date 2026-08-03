package br.com.gop.controllers;

import br.com.gop.models.CidadeRequest;
import br.com.gop.models.CidadeResponse;
import br.com.gop.models.ColaboradorRequest;
import br.com.gop.models.ColaboradorResponse;
import br.com.gop.models.EquipamentoRequest;
import br.com.gop.models.EquipamentoResponse;
import br.com.gop.models.EquipeRequest;
import br.com.gop.models.EquipeResponse;
import br.com.gop.models.RuaRequest;
import br.com.gop.models.RuaResponse;
import br.com.gop.models.SetorRequest;
import br.com.gop.models.SetorResponse;
import br.com.gop.models.TrechoRequest;
import br.com.gop.models.TrechoResponse;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.CidadeService;
import br.com.gop.services.ColaboradorService;
import br.com.gop.services.EquipamentoService;
import br.com.gop.services.EquipeService;
import br.com.gop.services.RuaService;
import br.com.gop.services.SetorService;
import br.com.gop.services.TrechoService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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

import java.util.List;

/**
 * Cadastros operacionais da Fase 1 (escopo empresa).
 * Shapefile e Frequência ficam para fatias seguintes.
 */
@RestController
@RequestMapping("/v1/org")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class CadastrosOperacionaisController {

	private final CidadeService cidadeService;
	private final SetorService setorService;
	private final RuaService ruaService;
	private final TrechoService trechoService;
	private final EquipeService equipeService;
	private final ColaboradorService colaboradorService;
	private final EquipamentoService equipamentoService;

	public CadastrosOperacionaisController(
		CidadeService cidadeService,
		SetorService setorService,
		RuaService ruaService,
		TrechoService trechoService,
		EquipeService equipeService,
		ColaboradorService colaboradorService,
		EquipamentoService equipamentoService
	) {
		this.cidadeService = cidadeService;
		this.setorService = setorService;
		this.ruaService = ruaService;
		this.trechoService = trechoService;
		this.equipeService = equipeService;
		this.colaboradorService = colaboradorService;
		this.equipamentoService = equipamentoService;
	}

	@GetMapping("/cidades")
	public List<CidadeResponse> listarCidades() {
		return cidadeService.listar(empresaId());
	}

	@GetMapping("/cidades/{id}")
	public CidadeResponse buscarCidade(@PathVariable Long id) {
		return cidadeService.buscar(empresaId(), id);
	}

	@PostMapping("/cidades")
	public ResponseEntity<CidadeResponse> criarCidade(@Valid @RequestBody CidadeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(cidadeService.criar(empresaId(), request));
	}

	@PutMapping("/cidades/{id}")
	public CidadeResponse atualizarCidade(@PathVariable Long id, @Valid @RequestBody CidadeRequest request) {
		return cidadeService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/cidades/{id}")
	public ResponseEntity<Void> desativarCidade(@PathVariable Long id) {
		cidadeService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/setores")
	public List<SetorResponse> listarSetores(@RequestParam(required = false) Long cidadeId) {
		return setorService.listar(empresaId(), cidadeId);
	}

	@GetMapping("/setores/{id}")
	public SetorResponse buscarSetor(@PathVariable Long id) {
		return setorService.buscar(empresaId(), id);
	}

	@PostMapping("/setores")
	public ResponseEntity<SetorResponse> criarSetor(@Valid @RequestBody SetorRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(setorService.criar(empresaId(), request));
	}

	@PutMapping("/setores/{id}")
	public SetorResponse atualizarSetor(@PathVariable Long id, @Valid @RequestBody SetorRequest request) {
		return setorService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/setores/{id}")
	public ResponseEntity<Void> desativarSetor(@PathVariable Long id) {
		setorService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/ruas")
	public List<RuaResponse> listarRuas(@RequestParam(required = false) Long cidadeId) {
		return ruaService.listar(empresaId(), cidadeId);
	}

	@GetMapping("/ruas/{id}")
	public RuaResponse buscarRua(@PathVariable Long id) {
		return ruaService.buscar(empresaId(), id);
	}

	@PostMapping("/ruas")
	public ResponseEntity<RuaResponse> criarRua(@Valid @RequestBody RuaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ruaService.criar(empresaId(), request));
	}

	@PutMapping("/ruas/{id}")
	public RuaResponse atualizarRua(@PathVariable Long id, @Valid @RequestBody RuaRequest request) {
		return ruaService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/ruas/{id}")
	public ResponseEntity<Void> desativarRua(@PathVariable Long id) {
		ruaService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/trechos")
	public List<TrechoResponse> listarTrechos(@RequestParam(required = false) Long ruaId) {
		return trechoService.listar(empresaId(), ruaId);
	}

	@GetMapping("/trechos/{id}")
	public TrechoResponse buscarTrecho(@PathVariable Long id) {
		return trechoService.buscar(empresaId(), id);
	}

	@PostMapping("/trechos")
	public ResponseEntity<TrechoResponse> criarTrecho(@Valid @RequestBody TrechoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(trechoService.criar(empresaId(), request));
	}

	@PutMapping("/trechos/{id}")
	public TrechoResponse atualizarTrecho(@PathVariable Long id, @Valid @RequestBody TrechoRequest request) {
		return trechoService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/trechos/{id}")
	public ResponseEntity<Void> desativarTrecho(@PathVariable Long id) {
		trechoService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/equipes")
	public List<EquipeResponse> listarEquipes(@RequestParam(required = false) Long setorId) {
		return equipeService.listar(empresaId(), setorId);
	}

	@GetMapping("/equipes/{id}")
	public EquipeResponse buscarEquipe(@PathVariable Long id) {
		return equipeService.buscar(empresaId(), id);
	}

	@PostMapping("/equipes")
	public ResponseEntity<EquipeResponse> criarEquipe(@Valid @RequestBody EquipeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.criar(empresaId(), request));
	}

	@PutMapping("/equipes/{id}")
	public EquipeResponse atualizarEquipe(@PathVariable Long id, @Valid @RequestBody EquipeRequest request) {
		return equipeService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/equipes/{id}")
	public ResponseEntity<Void> desativarEquipe(@PathVariable Long id) {
		equipeService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/colaboradores")
	public List<ColaboradorResponse> listarColaboradores(@RequestParam(required = false) Long equipeId) {
		return colaboradorService.listar(empresaId(), equipeId);
	}

	@GetMapping("/colaboradores/{id}")
	public ColaboradorResponse buscarColaborador(@PathVariable Long id) {
		return colaboradorService.buscar(empresaId(), id);
	}

	@PostMapping("/colaboradores")
	public ResponseEntity<ColaboradorResponse> criarColaborador(
		@Valid @RequestBody ColaboradorRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(colaboradorService.criar(empresaId(), request));
	}

	@PutMapping("/colaboradores/{id}")
	public ColaboradorResponse atualizarColaborador(
		@PathVariable Long id,
		@Valid @RequestBody ColaboradorRequest request
	) {
		return colaboradorService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/colaboradores/{id}")
	public ResponseEntity<Void> desativarColaborador(@PathVariable Long id) {
		colaboradorService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/equipamentos")
	public List<EquipamentoResponse> listarEquipamentos(@RequestParam(required = false) Long equipeId) {
		return equipamentoService.listar(empresaId(), equipeId);
	}

	@GetMapping("/equipamentos/{id}")
	public EquipamentoResponse buscarEquipamento(@PathVariable Long id) {
		return equipamentoService.buscar(empresaId(), id);
	}

	@PostMapping("/equipamentos")
	public ResponseEntity<EquipamentoResponse> criarEquipamento(
		@Valid @RequestBody EquipamentoRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(equipamentoService.criar(empresaId(), request));
	}

	@PutMapping("/equipamentos/{id}")
	public EquipamentoResponse atualizarEquipamento(
		@PathVariable Long id,
		@Valid @RequestBody EquipamentoRequest request
	) {
		return equipamentoService.atualizar(empresaId(), id, request);
	}

	@DeleteMapping("/equipamentos/{id}")
	public ResponseEntity<Void> desativarEquipamento(@PathVariable Long id) {
		equipamentoService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
