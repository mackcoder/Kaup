package com.kaup.service;

/**
 * Uma mensagem da conversa, no formato que a IA recebe.
 *
 * @param papel    {@link #USUARIO} ou {@link #ASSISTENTE}
 * @param conteudo texto da mensagem
 */
public record TurnoDeConversa(String papel, String conteudo) {

    public static final String USUARIO = "user";
    public static final String ASSISTENTE = "assistant";
}
