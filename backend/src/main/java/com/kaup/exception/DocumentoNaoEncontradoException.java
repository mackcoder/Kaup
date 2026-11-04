package com.kaup.exception;

/** Documento inexistente (ou identificador que nem é um UUID válido). */
public class DocumentoNaoEncontradoException extends RuntimeException {

    public DocumentoNaoEncontradoException() {
        super("Documento não encontrado.");
    }
}
