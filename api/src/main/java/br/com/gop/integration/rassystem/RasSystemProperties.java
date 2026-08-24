package br.com.gop.integration.rassystem;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gop.rassystem")
public class RasSystemProperties {

	public static final String BASE_URL_PADRAO = "https://backend.rassystem.com.br/app/v1/api";

	private String baseUrl = BASE_URL_PADRAO;
	private String login = "";
	private String senha = "";
	private String idFilial;
	private String idRotulo;
	private String idTipoServico;
	private boolean pollerEnabled = false;
	private long pollIntervalMs = 120_000L;
	private String empresaId;

	public String getBaseUrl() {
		if (baseUrl == null || baseUrl.isBlank()) {
			return BASE_URL_PADRAO;
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getLogin() {
		return login == null ? "" : login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getSenha() {
		return senha == null ? "" : senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public Integer getIdFilial() {
		return parseInteger(idFilial);
	}

	public void setIdFilial(String idFilial) {
		this.idFilial = idFilial;
	}

	public Integer getIdRotulo() {
		return parseInteger(idRotulo);
	}

	public void setIdRotulo(String idRotulo) {
		this.idRotulo = idRotulo;
	}

	public Integer getIdTipoServico() {
		return parseInteger(idTipoServico);
	}

	public void setIdTipoServico(String idTipoServico) {
		this.idTipoServico = idTipoServico;
	}

	public boolean isPollerEnabled() {
		return pollerEnabled;
	}

	public void setPollerEnabled(boolean pollerEnabled) {
		this.pollerEnabled = pollerEnabled;
	}

	public long getPollIntervalMs() {
		return pollIntervalMs;
	}

	public void setPollIntervalMs(long pollIntervalMs) {
		this.pollIntervalMs = pollIntervalMs;
	}

	public Long getEmpresaId() {
		return parseLong(empresaId);
	}

	public void setEmpresaId(String empresaId) {
		this.empresaId = empresaId;
	}

	public boolean isConfigurado() {
		return !getLogin().isBlank() && !getSenha().isBlank();
	}

	private static Integer parseInteger(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return Integer.valueOf(valor.strip());
	}

	private static Long parseLong(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return Long.valueOf(valor.strip());
	}
}
