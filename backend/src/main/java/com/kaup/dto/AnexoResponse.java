package com.kaup.dto;

import java.time.Instant;

/**
 * Anexo de contexto de um documento.
 *
 * @param id         texto (o frontend rejeita número)
 * @param documentId o mesmo id da URL
 */
public record AnexoResponse(
        String id,
        String documentId,
        String name,
        String mimeType,
        Instant createdAt) { }
