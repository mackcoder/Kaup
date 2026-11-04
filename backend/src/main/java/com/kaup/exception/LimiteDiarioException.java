package com.kaup.exception;

/** O teto diário de chamadas à IA acabou: tente amanhã (vira HTTP 429). */
public class LimiteDiarioException extends RuntimeException {

    private final long segundos;

    public LimiteDiarioException(long segundos) {
        super("O Kaup atingiu o limite de uso de hoje. Tente amanhã.");
        this.segundos = segundos;
    }

    /** Segundos até o limite zerar (meia-noite), no mínimo 1. */
    public long getSegundos() { return segundos; }
}
