package com.kaup.exception;

/** Requisição com arquivo ou texto de contrato inválido (erro do cliente). */
public class ContratoInvalidoException extends RuntimeException {

    public ContratoInvalidoException(String mensagem) {
        super(mensagem);
    }
}