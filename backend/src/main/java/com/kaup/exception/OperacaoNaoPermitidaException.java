package com.kaup.exception;

/** A pessoa está logada, mas não pode fazer isso (vira HTTP 403). */
public class OperacaoNaoPermitidaException extends RuntimeException {

    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
