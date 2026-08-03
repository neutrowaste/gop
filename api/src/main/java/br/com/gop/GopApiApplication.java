package br.com.gop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @EnableScheduling} habilita o SaudeGpsScheduler (RN-001: silêncio de GPS). */
@SpringBootApplication
@EnableScheduling
public class GopApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GopApiApplication.class, args);
	}

}
