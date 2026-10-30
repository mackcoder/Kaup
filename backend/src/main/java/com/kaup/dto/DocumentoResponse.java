package com.kaup.dto;

import java.time.Instant;

/**
 * Documento como o frontend espera (ver frontend/src/services/api/dto.ts).
 * Os nomes dos campos são o contrato: não renomear sem ajustar o frontend.
 *
 * @param id               UUID em texto (o frontend rejeita número)
 * @param categoryId       sempre presente na resposta; nulo quando não há categoria
 * @param processingStatus pending, processing, completed ou failed
 * @param description      primeira frase do resumo, ou nulo
 * @param visualStatus     "ok" ou "warning"
 */
public record DocumentoResponse(
        String id,
        String name,
        String categoryId,
        String processingStatus,
        Instant createdAt,
        String description,
        String visualStatus) { }
