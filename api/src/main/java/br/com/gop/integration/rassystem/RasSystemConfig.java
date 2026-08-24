package br.com.gop.integration.rassystem;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RasSystemProperties.class)
public class RasSystemConfig {

	@Bean(name = "rasSystemRestClient")
	RestClient rasSystemRestClient(RasSystemProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(Duration.ofSeconds(60));
		return RestClient.builder()
			.baseUrl(properties.getBaseUrl())
			.requestFactory(factory)
			.build();
	}
}
