package com.kaup.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Contrato do provedor de IA. O restante do sistema depende só desta interface,
 * então trocar de provedor (Gemini, OpenAI, Anthropic...) é criar uma nova
 * implementação e escolhê-la em {@code ia.provedor}.
 */
public interface ServicoDeIA {

    /**
     * Interpreta o texto de um contrato e devolve um JSON com as chaves
     * {@code resumo}, {@code valores}, {@code prazos}, {@code multasEJuros},
     * {@code obrigacoes} e {@code pontosDeAtencao}.
     */
    JsonNode interpretar(String textoContrato);
}
