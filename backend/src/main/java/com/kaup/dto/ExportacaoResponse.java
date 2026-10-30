package com.kaup.dto;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tudo o que o sistema guarda de uma pessoa, para ela baixar. Nunca inclui a senha (nem o hash).
 * O arquivo original dos contratos não é guardado, então só o texto extraído e a análise vão aqui.
 */
public record ExportacaoResponse(String exportedAt, Conta account, List<Documento> documents) {

    public record Conta(String name, String email, String role, String createdAt, Preferencias preferences) { }

    public record Preferencias(String fontSize, String explainLevel, String income, boolean audio,
                               boolean shareIncomeWithAi) { }

    public record Documento(String id, String name, String categoryId, String mimeType, String processingStatus,
                            String createdAt, String extractedText, JsonNode analysis, List<Mensagem> messages) { }

    public record Mensagem(String role, String text, String createdAt) { }
}
