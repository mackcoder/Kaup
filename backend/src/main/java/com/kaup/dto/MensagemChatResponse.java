package com.kaup.dto;

import java.time.Instant;

/**
 * Mensagem do chat como o frontend espera (ver messageFromDTO em
 * frontend/src/services/api/dto.ts).
 *
 * @param id         texto (o frontend rejeita número)
 * @param documentId o mesmo id da URL; o frontend recusa a mensagem se divergir
 * @param role       "user" ou "assistant"
 */
public record MensagemChatResponse(
        String id,
        String documentId,
        String role,
        String content,
        Instant createdAt) { }
