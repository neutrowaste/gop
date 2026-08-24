package br.com.gop.services;

/**
 * Falha ao falar com um sistema externo (ACL). Não mistura payload cru
 * da integração com o modelo interno.
 */
public class IntegracaoExternaException extends RuntimeException {

	private final int statusHttp;

	public IntegracaoExternaException(String mensagem) {
		this(mensagem, 502);
	}

	public IntegracaoExternaException(String mensagem, int statusHttp) {
		super(mensagem);
		this.statusHttp = statusHttp;
	}

	public IntegracaoExternaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
		this.statusHttp = 502;
	}

	public int getStatusHttp() {
		return statusHttp;
	}
}
