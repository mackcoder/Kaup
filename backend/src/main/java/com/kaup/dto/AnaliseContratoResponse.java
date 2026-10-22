package com.kaup.dito;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resposta da análise de um contrato.
 *
 * @param textoExtraido texto lido do arquivo (OCR), útil para o usuário conferir
 * @param analise       interpretação estruturada devolvida pela IA
 */

public record AnaliseContratoResponse(String textoExtraido, JsonNode analise) { } 
