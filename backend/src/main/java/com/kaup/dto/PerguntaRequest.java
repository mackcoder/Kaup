package com.kaup.dto;

/** Corpo do POST do chat: {@code {"message": "..."}} (formato do frontend). */
public record PerguntaRequest(String message) { }
