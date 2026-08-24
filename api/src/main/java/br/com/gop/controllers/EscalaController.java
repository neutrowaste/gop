package br.com.gop.controllers;

import br.com.gop.models.EscalaRequest;
import br.com.gop.models.EscalaResponse;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.TipoEscala;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.EscalaService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Módulo Escala (RN-001): amarração diária obrigatória
 * Setor ↔ Colaboradores (Lutocar designado) ↔ Supervisor ↔ Turno.
 * Pré-requisito do motor de regras da Varrição.
 */
@RestController
@RequestMapping("/v1/org/escalas")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class EscalaController {

	private final EscalaService escalaService;

	public EscalaController(EscalaService escalaService) {
		this.escalaService = escalaService;
	}

	@GetMapping
	public List<EscalaResponse> listar(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
		@RequestParam(required = false) Long setorId,
		@RequestParam(required = false) StatusEscala status,
		@RequestParam(required = false) TipoEscala tipo
	) {
		return escalaService.listar(empresaId(), data, setorId, status, tipo);
	}

	@GetMapping("/{id}")
	public EscalaResponse buscar(@PathVariable Long id) {
		return escalaService.buscar(empresaId(), id);
	}

	@PostMapping
	public ResponseEntity<EscalaResponse> criar(@Valid @RequestBody EscalaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(escalaService.criar(empresaId(), request));
	}

	@PutMapping("/{id}")
	public EscalaResponse atualizar(@PathVariable Long id, @Valid @RequestBody EscalaRequest request) {
		return escalaService.atualizar(empresaId(), id, request);
	}

	@PostMapping("/{id}/publicar")
	public EscalaResponse publicar(@PathVariable Long id) {
		return escalaService.publicar(empresaId(), id);
	}

	@PostMapping("/{id}/despublicar")
	public EscalaResponse despublicar(@PathVariable Long id) {
		return escalaService.despublicar(empresaId(), id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancelar(@PathVariable Long id) {
		escalaService.desativar(empresaId(), id);
		return ResponseEntity.noContent().build();
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
