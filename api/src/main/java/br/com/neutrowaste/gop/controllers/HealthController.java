package br.com.neutrowaste.gop.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
public class HealthController {

	@GetMapping
	public ResponseEntity<Map<String, Object>> health() {
		return ResponseEntity.ok(Map.of(
			"status", "UP",
			"service", "gop-api",
			"timestamp", Instant.now().toString()
		));
	}
}
