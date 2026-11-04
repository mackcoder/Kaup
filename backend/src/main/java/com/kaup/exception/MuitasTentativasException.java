package com.kaup.exception;

/** Muitos erros seguidos: tente de novo em instantes (vira HTTP 429). */
public class MuitasTentativasException extends RuntimeException {

    private final long segundos;

    public MuitasTentativasException(long segundos) {
        super("Muitas tentativas. Aguarde alguns segundos e tente de novo.");
        this.segundos = segundos;
    }

    /** Quanto falta para a pessoa poder tentar de novo (arredondado para cima, mínimo 1). */
    public long getSegundos() { return segundos; }
}
