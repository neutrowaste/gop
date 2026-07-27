package br.com.gop.controllers;

import br.com.gop.models.EmpresaModuloRequest;
import br.com.gop.models.EmpresaModuloResponse;
import br.com.gop.services.EmpresaModuloService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/empresas/{empresaId}/modulos")
@Profile("postgres")
@PreAuthorize(
	"hasRole('ADMIN_PLATAFORMA') or hasAuthority('PLATAFORMA_EMPRESA_GERENCIAR')"
)
public class EmpresaModuloAdminController {

	private final EmpresaModuloService empresaModuloService;

	public EmpresaModuloAdminController(EmpresaModuloService empresaModuloService) {
		this.empresaModuloService = empresaModuloService;
	}

	@GetMapping
	public List<EmpresaModuloResponse> listar(@PathVariable Long empresaId) {
		return empresaModuloService.listarPorEmpresa(empresaId);
	}

	@PutMapping("/{moduloId}")
	public EmpresaModuloResponse atualizar(
		@PathVariable Long empresaId,
		@PathVariable Long moduloId,
		@Valid @RequestBody EmpresaModuloRequest request
	) {
		return empresaModuloService.atualizar(empresaId, moduloId, request);
	}
}
