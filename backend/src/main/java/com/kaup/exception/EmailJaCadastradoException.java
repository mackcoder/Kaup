package com.kaup.exception;

/** Já existe uma conta com esse e-mail (vira HTTP 409). */
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("Já existe uma conta com esse e-mail.");
    }
}
