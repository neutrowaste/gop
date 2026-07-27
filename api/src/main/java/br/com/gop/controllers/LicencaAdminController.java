package br.com.gop.controllers;

import br.com.gop.models.LicencaRequest;
import br.com.gop.models.LicencaResponse;
import br.com.gop.models.TetoAtivosResponse;
import br.com.gop.services.ControleTetoAtivosService;
import br.com.gop.services.LicencaService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin")
@Profile("postgres")
@PreAuthorize(
	"hasRole('ADMIN_PLATAFORMA') or hasAuthority('PLATAFORMA_LICENCA_GERENCIAR')"
)
public class LicencaAdminController {

	private final LicencaService licencaService;
	private final ControleTetoAtivosService controleTetoAtivosService;

	public LicencaAdminController(
		LicencaService licencaService,
		ControleTetoAtivosService controleTetoAtivosService
	) {
		this.licencaService = licencaService;
		this.controleTetoAtivosService = controleTetoAtivosService;
	}

	@GetMapping("/empresas/{empresaId}/licencas")
	public List<LicencaResponse> listar(@PathVariable Long empresaId) {
		return licencaService.listarPorEmpresa(empresaId);
	}

	@PostMapping("/empresas/{empresaId}/licencas")
	public ResponseEntity<LicencaResponse> criar(
		@PathVariable Long empresaId,
		@Valid @RequestBody LicencaRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(licencaService.criar(empresaId, request));
	}

	@GetMapping("/licencas/{id}")
	public LicencaResponse buscar(@PathVariable Long id) {
		return licencaService.buscar(id);
	}

	@PutMapping("/licencas/{id}")
	public LicencaResponse atualizar(
		@PathVariable Long id,
		@Valid @RequestBody LicencaRequest request
	) {
		return licencaService.atualizar(id, request);
	}

	@DeleteMapping("/licencas/{id}")
	public ResponseEntity<Void> encerrar(@PathVariable Long id) {
		licencaService.encerrar(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/empresas/{empresaId}/teto-ativos")
	public TetoAtivosResponse consultarTeto(@PathVariable Long empresaId) {
		return controleTetoAtivosService.consultar(empresaId);
	}
}
