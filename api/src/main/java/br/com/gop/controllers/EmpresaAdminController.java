package br.com.gop.controllers;

import br.com.gop.models.EmpresaRequest;
import br.com.gop.models.EmpresaResponse;
import br.com.gop.services.EmpresaService;
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

@RestController
@RequestMapping("/v1/admin/empresas")
@Profile("postgres")
@PreAuthorize(
	"hasRole('ADMIN_PLATAFORMA') or hasAuthority('PLATAFORMA_EMPRESA_GERENCIAR')"
)
public class EmpresaAdminController {

	private final EmpresaService empresaService;

	public EmpresaAdminController(EmpresaService empresaService) {
		this.empresaService = empresaService;
	}

	@GetMapping
	public List<EmpresaResponse> listar(@RequestParam(required = false) Boolean ativo) {
		return empresaService.listar(ativo);
	}

	@GetMapping("/{id}")
	public EmpresaResponse buscar(@PathVariable Long id) {
		return empresaService.buscar(id);
	}

	@PostMapping
	public ResponseEntity<EmpresaResponse> criar(@Valid @RequestBody EmpresaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.criar(request));
	}

	@PutMapping("/{id}")
	public EmpresaResponse atualizar(
		@PathVariable Long id,
		@Valid @RequestBody EmpresaRequest request
	) {
		return empresaService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> desativar(@PathVariable Long id) {
		empresaService.desativar(id);
		return ResponseEntity.noContent().build();
	}
}
