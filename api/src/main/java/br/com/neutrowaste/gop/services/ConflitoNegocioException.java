package br.com.neutrowaste.gop.services;

public class ConflitoNegocioException extends RuntimeException {

	public ConflitoNegocioException(String mensagem) {
		super(mensagem);
	}
}
