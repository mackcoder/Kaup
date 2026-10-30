package com.kaup.dto;

import java.util.List;

/**
 * Análise de um documento como o frontend espera (ver analysisFromDTO em
 * frontend/src/services/api/dto.ts).
 *
 * @param summary           resumo em linguagem simples, ou nulo
 * @param ocrQualityWarning aviso de leitura ruim; hoje sempre nulo (ainda não calculado)
 */
public record AnaliseResponse(
        String documentId,
        String processingStatus,
        String summary,
        List<PontoChave> keyPoints,
        String ocrQualityWarning) {

    /** @param severity "info", "warning" ou "critical" */
    public record PontoChave(String id, String title, String description, String severity) { }
}
