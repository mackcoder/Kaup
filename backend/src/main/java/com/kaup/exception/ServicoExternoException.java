package com.kaup.exception;

/** Falha ao falar com um serviço externo (OCR ou IA). */
public class ServicoExternoException extends RuntimeException {

    public ServicoExternoException(String servico, Throwable causa) {
        super("Falha ao comunicar com o serviço de " + servico + ".", causa);
    }
}