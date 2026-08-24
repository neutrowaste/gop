package br.com.gop.controllers;

import br.com.gop.integration.rassystem.RasFilial;
import br.com.gop.integration.rassystem.RasRotulo;
import br.com.gop.integration.rassystem.RasVeiculo;
import br.com.gop.models.RasSystemImportarCadastroResponse;
import br.com.gop.models.RasSystemImportarRastreioRequest;
import br.com.gop.models.RasSystemImportarRastreioResponse;
import br.com.gop.models.RasSystemLutocarResponse;
import br.com.gop.models.RasSystemStatusResponse;
import br.com.gop.models.RasSystemSyncResponse;
import br.com.gop.models.TelemetriaGps;
import br.com.gop.security.SegurancaContexto;
import br.com.gop.services.RasSystemIngestaoService;
import br.com.gop.services.RasSystemLeituraService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ACL RasSystem: leitura da frota externa e aplicação no modelo interno GOP.
 * GPS próprio futuro usa o mesmo contrato ({@link TelemetriaGps} + POST /v1/org/gps/posicoes).
 */
@RestController
@RequestMapping("/v1/org/integracoes/rassystem")
@Profile("postgres")
@PreAuthorize("hasRole('ADMIN_EMPRESA') or hasAuthority('EMPRESA_USUARIO_GERENCIAR')")
public class RasSystemController {

	private final RasSystemLeituraService leituraService;
	private final RasSystemIngestaoService ingestaoService;

	public RasSystemController(
		RasSystemLeituraService leituraService,
		RasSystemIngestaoService ingestaoService
	) {
		this.leituraService = leituraService;
		this.ingestaoService = ingestaoService;
	}

	@GetMapping("/status")
	public RasSystemStatusResponse status() {
		return leituraService.status();
	}

	@GetMapping("/filiais")
	public List<RasFilial> filiais() {
		return leituraService.filiais();
	}

	@GetMapping("/filiais/{idFilial}/rotulos")
	public List<RasRotulo> rotulos(@PathVariable long idFilial) {
		return leituraService.rotulos(idFilial);
	}

	@GetMapping("/rotulos/{idRotulo}/veiculos")
	public List<RasVeiculo> veiculos(@PathVariable long idRotulo) {
		return leituraService.veiculos(idRotulo);
	}

	@GetMapping("/lutocares")
	public List<RasSystemLutocarResponse> lutocares(
		@RequestParam(required = false) Integer idFilial,
		@RequestParam(required = false) Integer idRotulo
	) {
		return ingestaoService.listarLutocares(empresaId(), idFilial, idRotulo);
	}

	@GetMapping("/ultimas-coordenadas")
	public List<TelemetriaGps> ultimasCoordenadas(
		@RequestParam(required = false) Integer idFilial,
		@RequestParam(required = false) Integer idRotulo,
		@RequestParam(required = false) Integer idTipoServico
	) {
		return leituraService.ultimasCoordenadas(idFilial, idRotulo, idTipoServico);
	}

	@GetMapping("/veiculos/{idVeiculo}/rastreio")
	public List<TelemetriaGps> rastreio(
		@PathVariable long idVeiculo,
		@RequestParam String dataInicio,
		@RequestParam String dataFim
	) {
		return leituraService.rastreio(idVeiculo, dataInicio, dataFim);
	}

	@GetMapping("/veiculos/{idVeiculo}/eventos")
	public List<TelemetriaGps> eventos(
		@PathVariable long idVeiculo,
		@RequestParam String dataInicio,
		@RequestParam String dataFim
	) {
		return leituraService.eventos(idVeiculo, dataInicio, dataFim);
	}

	@PostMapping("/importar-cadastro")
	public RasSystemImportarCadastroResponse importarCadastro(
		@RequestParam(required = false) Integer limite,
		@RequestParam(required = false) Integer idFilial,
		@RequestParam(required = false) Integer idRotulo
	) {
		return ingestaoService.importarCadastro(empresaId(), limite, idFilial, idRotulo);
	}

	@PostMapping("/sync")
	public RasSystemSyncResponse sync(
		@RequestParam(required = false) Integer idFilial,
		@RequestParam(required = false) Integer idRotulo
	) {
		return ingestaoService.sincronizar(empresaId(), idFilial, idRotulo);
	}

	@PostMapping("/importar-rastreio")
	public ResponseEntity<RasSystemImportarRastreioResponse> importar(
		@Valid @RequestBody RasSystemImportarRastreioRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ingestaoService.importar(empresaId(), request));
	}

	private Long empresaId() {
		return SegurancaContexto.empresaIdObrigatoria();
	}
}
