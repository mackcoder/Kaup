package com.kaup.service;

import java.util.List;
import java.util.Map;

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

    /**
     * Escolhe a categoria do contrato entre as informadas.
     *
     * @param categorias chave -> descrição curta de cada categoria possível
     * @return a chave que a IA escolheu, exatamente como ela respondeu (pode vir vazia ou
     *         fora da lista: quem chama valida)
     */
    String classificar(String textoContrato, Map<String, String> categorias);

    /**
     * Responde uma pergunta do usuário sobre o contrato, em texto simples.
     *
     * @param historico mensagens anteriores da conversa, da mais antiga para a mais recente
     * @return o texto da resposta, nunca nulo
     */
    String conversar(String textoContrato, List<TurnoDeConversa> historico, String pergunta);
}
