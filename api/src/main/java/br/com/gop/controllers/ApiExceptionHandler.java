package br.com.gop.controllers;

import br.com.gop.services.ConflitoNegocioException;
import br.com.gop.services.IntegracaoExternaException;
import br.com.gop.services.RecursoNaoEncontradoException;
import br.com.gop.services.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail credenciaisInvalidas() {
		ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problema.setTitle("Não autorizado");
		problema.setDetail("Email ou senha inválidos");
		return problema;
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail acessoNegado() {
		return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Acesso negado");
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

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ProblemDetail arquivoGrande() {
		return problema(
			HttpStatus.PAYLOAD_TOO_LARGE,
			"Arquivo grande demais",
			"O shapefile excede o limite de 50 MB"
		);
	}

	@ExceptionHandler(IntegracaoExternaException.class)
	ProblemDetail integracaoExterna(IntegracaoExternaException exception) {
		LOGGER.warn("Falha de integração externa: {}", exception.getMessage());
		return problema(HttpStatus.BAD_GATEWAY, "Falha na integração externa", exception.getMessage());
	}

	@ExceptionHandler(DataAccessException.class)
	ProblemDetail falhaDeDados(DataAccessException exception) {
		LOGGER.error("Falha de acesso a dados", exception);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Falha ao consultar ou gravar dados");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail inesperado(Exception exception) {
		LOGGER.error("Erro não tratado", exception);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Falha ao processar a requisição");
	}

	private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatus(status);
		problema.setTitle(titulo);
		problema.setDetail(detalhe);
		return problema;
	}
}
