package com.kaup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Converte exceções em respostas JSON (RFC 7807) sem vazar detalhes internos. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ContratoInvalidoException.class)
    public ProblemDetail contratoInvalido(ContratoInvalidoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DocumentoNaoEncontradoException.class)
    public ProblemDetail documentoNaoEncontrado(DocumentoNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail arquivoGrande(MaxUploadSizeExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo excede o tamanho máximo permitido.");
    }

    @ExceptionHandler(ServicoExternoException.class)
    public ProblemDetail servicoExterno(ServicoExternoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }
}
