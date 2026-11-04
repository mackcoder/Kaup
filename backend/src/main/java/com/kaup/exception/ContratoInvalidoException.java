package com.kaup.exception;

/** Requisição com arquivo, texto de contrato ou pergunta inválidos (erro do cliente). */
public class ContratoInvalidoException extends DadosInvalidosException {

    public ContratoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
