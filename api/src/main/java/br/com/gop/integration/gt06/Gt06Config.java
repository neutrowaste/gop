package br.com.gop.integration.gt06;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("postgres")
@EnableConfigurationProperties(Gt06Properties.class)
public class Gt06Config {
}
