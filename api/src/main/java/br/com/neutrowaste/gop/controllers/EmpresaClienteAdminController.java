package br.com.neutrowaste.gop.controllers;

import br.com.neutrowaste.gop.models.EmpresaAdminRequest;
import br.com.neutrowaste.gop.models.EmpresaAdminResponse;
import br.com.neutrowaste.gop.models.EmpresaModuloRequest;
import br.com.neutrowaste.gop.models.EmpresaModuloResponse;
import br.com.neutrowaste.gop.services.EmpresaAdminUsuarioService;
import br.com.neutrowaste.gop.services.EmpresaModuloService;
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
@RequestMapping("/v1/admin/empresas/{empresaId}")
@Profile("postgres")
@PreAuthorize(
	"hasRole('ADMIN_PLATAFORMA') or hasAuthority('PLATAFORMA_EMPRESA_GERENCIAR')"
)
public class EmpresaClienteAdminController {

	private final EmpresaAdminUsuarioService empresaAdminUsuarioService;
	private final EmpresaModuloService empresaModuloService;

	public EmpresaClienteAdminController(
		EmpresaAdminUsuarioService empresaAdminUsuarioService,
		EmpresaModuloService empresaModuloService
	) {
		this.empresaAdminUsuarioService = empresaAdminUsuarioService;
		this.empresaModuloService = empresaModuloService;
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

	@GetMapping("/modulos")
	public List<EmpresaModuloResponse> listarModulos(@PathVariable Long empresaId) {
		return empresaModuloService.listarPorEmpresa(empresaId);
	}

	@PutMapping("/modulos/{moduloId}")
	public EmpresaModuloResponse atualizarModulo(
		@PathVariable Long empresaId,
		@PathVariable Long moduloId,
		@Valid @RequestBody EmpresaModuloRequest request
	) {
		return empresaModuloService.atualizar(empresaId, moduloId, request);
	}
}
