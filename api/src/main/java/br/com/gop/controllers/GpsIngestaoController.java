package br.com.gop.controllers;

import br.com.gop.models.PosicaoGpsRequest;
import br.com.gop.models.PosicaoGpsResponse;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.GpsIngestaoService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestão de posições GPS (RN-001, fase 2: síncrona, autenticação JWT padrão).
 * TODO(fase de integração externa): autenticação dedicada por dispositivo
 * (API key / adaptador ACL para RasSystem, GPS próprio) substituirá o JWT de
 * usuário aqui — fora do escopo desta entrega, focada no motor de regras.
 */
@RestController
@RequestMapping("/v1/org/gps")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class GpsIngestaoController {

	private final GpsIngestaoService gpsIngestaoService;

	public GpsIngestaoController(GpsIngestaoService gpsIngestaoService) {
		this.gpsIngestaoService = gpsIngestaoService;
	}

	@PostMapping("/posicoes")
	public ResponseEntity<PosicaoGpsResponse> ingerir(@Valid @RequestBody PosicaoGpsRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(gpsIngestaoService.ingerir(empresaId(), request));
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
