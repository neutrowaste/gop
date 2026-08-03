package br.com.gop.controllers;

import br.com.gop.models.ParametroEmpresaRequest;
import br.com.gop.models.ParametroEmpresaResponse;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.ParametroEmpresaService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Limiares do motor de Varrição (RN-001) configuráveis por empresa
 * (cobertura mínima, buffer de snap, silêncio de GPS etc).
 */
@RestController
@RequestMapping("/v1/org/parametros")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class ParametroEmpresaController {

	private final ParametroEmpresaService parametroEmpresaService;

	public ParametroEmpresaController(ParametroEmpresaService parametroEmpresaService) {
		this.parametroEmpresaService = parametroEmpresaService;
	}

	@GetMapping
	public List<ParametroEmpresaResponse> listar() {
		return parametroEmpresaService.listar(empresaId());
	}

	@PutMapping("/{chave}")
	public ParametroEmpresaResponse definir(
		@PathVariable String chave,
		@Valid @RequestBody ParametroEmpresaRequest request
	) {
		return parametroEmpresaService.definir(empresaId(), chave, request);
	}

	@DeleteMapping("/{chave}")
	public ResponseEntity<Void> restaurarPadrao(@PathVariable String chave) {
		parametroEmpresaService.restaurarPadrao(empresaId(), chave);
		return ResponseEntity.noContent().build();
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
