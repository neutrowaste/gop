package br.com.gop.integration.gt06;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gop.gps.tcp")
public class Gt06Properties {

	private boolean enabled = true;
	private String host = "0.0.0.0";
	private int port = 5023;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHost() {
		return host == null || host.isBlank() ? "0.0.0.0" : host.strip();
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
