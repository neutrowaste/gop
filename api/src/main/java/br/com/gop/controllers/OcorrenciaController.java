package br.com.gop.controllers;

import br.com.gop.models.OcorrenciaResolucaoRequest;
import br.com.gop.models.OcorrenciaResponse;
import br.com.gop.models.StatusOcorrencia;
import br.com.gop.models.TipoOcorrencia;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.OcorrenciaService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Ocorrências automáticas do motor de Varrição (RN-001): ordem do plano
 * pulada e GPS em silêncio.
 */
@RestController
@RequestMapping("/v1/org/ocorrencias")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class OcorrenciaController {

	private final OcorrenciaService ocorrenciaService;

	public OcorrenciaController(OcorrenciaService ocorrenciaService) {
		this.ocorrenciaService = ocorrenciaService;
	}

	@GetMapping
	public List<OcorrenciaResponse> listar(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
		@RequestParam(required = false) Long setorId,
		@RequestParam(required = false) Long equipeId,
		@RequestParam(required = false) StatusOcorrencia status,
		@RequestParam(required = false) TipoOcorrencia tipo
	) {
		return ocorrenciaService.listar(empresaId(), data, setorId, equipeId, status, tipo);
	}

	@GetMapping("/{id}")
	public OcorrenciaResponse buscar(@PathVariable Long id) {
		return ocorrenciaService.buscar(empresaId(), id);
	}

	@PostMapping("/{id}/resolver")
	public OcorrenciaResponse resolver(@PathVariable Long id, @Valid @RequestBody OcorrenciaResolucaoRequest request) {
		return ocorrenciaService.resolver(empresaId(), id, request);
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
