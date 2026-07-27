package br.com.gop.controllers;

import br.com.gop.models.EmpresaAdminRequest;
import br.com.gop.models.EmpresaAdminResponse;
import br.com.gop.services.EmpresaAdminUsuarioService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/empresas/{empresaId}")
@Profile("postgres")
@PreAuthorize(
	"hasRole('ADMIN_PLATAFORMA') or hasAuthority('PLATAFORMA_EMPRESA_GERENCIAR')"
)
public class EmpresaClienteAdminController {

	private final EmpresaAdminUsuarioService empresaAdminUsuarioService;

	public EmpresaClienteAdminController(EmpresaAdminUsuarioService empresaAdminUsuarioService) {
		this.empresaAdminUsuarioService = empresaAdminUsuarioService;
	}

	@GetMapping("/admins")
	public List<EmpresaAdminResponse> listarAdmins(@PathVariable Long empresaId) {
		return empresaAdminUsuarioService.listar(empresaId);
	}

	@PostMapping("/admins")
	public ResponseEntity<EmpresaAdminResponse> criarAdmin(
		@PathVariable Long empresaId,
		@Valid @RequestBody EmpresaAdminRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(empresaAdminUsuarioService.criar(empresaId, request));
	}

	@DeleteMapping("/admins/{usuarioId}")
	public ResponseEntity<Void> desativarAdmin(
		@PathVariable Long empresaId,
		@PathVariable Long usuarioId
	) {
		empresaAdminUsuarioService.desativar(empresaId, usuarioId);
		return ResponseEntity.noContent().build();
	}
}
