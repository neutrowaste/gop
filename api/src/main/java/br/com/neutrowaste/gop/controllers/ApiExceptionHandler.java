package br.com.neutrowaste.gop.controllers;

import br.com.neutrowaste.gop.services.ConflitoNegocioException;
import br.com.neutrowaste.gop.services.RecursoNaoEncontradoException;
import br.com.neutrowaste.gop.services.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail credenciaisInvalidas() {
		ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problema.setTitle("Não autorizado");
		problema.setDetail("Email ou senha inválidos");
		return problema;
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException exception) {
		return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage());
	}

	@ExceptionHandler({ConflitoNegocioException.class, DataIntegrityViolationException.class})
	ProblemDetail conflito(Exception exception) {
		String detalhe = exception instanceof ConflitoNegocioException
			? exception.getMessage()
			: "A operação conflita com dados já existentes";
		return problema(HttpStatus.CONFLICT, "Conflito de dados", detalhe);
	}

	@ExceptionHandler(RegraNegocioException.class)
	ProblemDetail regraNegocio(RegraNegocioException exception) {
		return problema(
			HttpStatus.UNPROCESSABLE_ENTITY,
			"Regra de negócio não atendida",
			exception.getMessage()
		);
	}

	private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatus(status);
		problema.setTitle(titulo);
		problema.setDetail(detalhe);
		return problema;
	}
}
