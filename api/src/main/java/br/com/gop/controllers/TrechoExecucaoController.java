package br.com.gop.controllers;

import br.com.gop.models.TrechoExecucaoReordenarRequest;
import br.com.gop.models.TrechoExecucaoRequest;
import br.com.gop.models.TrechoExecucaoResponse;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.TrechoExecucaoService;
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

/**
 * Plano diário de trechos da Escala (RN-001): quais trechos e em que ordem
 * a equipe deve varrer no dia/turno amarrado.
 */
@RestController
@RequestMapping("/v1/org/escalas/{escalaId}/trechos")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class TrechoExecucaoController {

	private final TrechoExecucaoService trechoExecucaoService;

	public TrechoExecucaoController(TrechoExecucaoService trechoExecucaoService) {
		this.trechoExecucaoService = trechoExecucaoService;
	}

	@GetMapping
	public List<TrechoExecucaoResponse> listar(@PathVariable Long escalaId) {
		return trechoExecucaoService.listar(empresaId(), escalaId);
	}

	@PostMapping
	public ResponseEntity<TrechoExecucaoResponse> adicionar(
		@PathVariable Long escalaId,
		@Valid @RequestBody TrechoExecucaoRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(trechoExecucaoService.adicionar(empresaId(), escalaId, request));
	}

	@PutMapping("/{id}")
	public TrechoExecucaoResponse atualizarOrdem(
		@PathVariable Long escalaId,
		@PathVariable Long id,
		@Valid @RequestBody TrechoExecucaoReordenarRequest request
	) {
		return trechoExecucaoService.atualizarOrdem(empresaId(), escalaId, id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long escalaId, @PathVariable Long id) {
		trechoExecucaoService.remover(empresaId(), escalaId, id);
		return ResponseEntity.noContent().build();
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
