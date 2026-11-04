package com.kaup.exception;

/** A fila de leitura de contratos está cheia: tente em instantes (vira HTTP 429). */
public class FilaCheiaException extends RuntimeException {

    public FilaCheiaException() {
        super("Estamos processando muitos contratos, tente em instantes.");
    }
}
