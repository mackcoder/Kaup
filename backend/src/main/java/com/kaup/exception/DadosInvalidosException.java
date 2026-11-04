package com.kaup.exception;

/** Dados enviados pelo cliente que não passam na validação (vira HTTP 400). */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
