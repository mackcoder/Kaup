package com.kaup.service;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaup.dto.AnaliseResponse.PontoChave;

/**
 * Traduz o JSON devolvido pela IA ({@code resumo}, {@code valores}, {@code prazos},
 * {@code multasEJuros}, {@code obrigacoes}, {@code pontosDeAtencao}) para o formato
 * do frontend. Tolerante a campos ausentes: a IA pode omitir qualquer um deles.
 */
public final class AnaliseMapper {

    private static final int TAMANHO_DESCRICAO = 140;

    private AnaliseMapper() { }

    /** Resumo da IA, ou nulo se ausente ou em branco. */
    public static String resumo(JsonNode analise) {
        return texto(analise.path("resumo"));
    }

    /** Primeira frase do resumo, truncada, para o cartão do documento. */
    public static String descricao(JsonNode analise) {
        String resumo = resumo(analise);
        if (resumo == null) {
            return null;
        }
        int fimDaFrase = resumo.indexOf(". ");
        String frase = fimDaFrase > 0 ? resumo.substring(0, fimDaFrase + 1) : resumo;
        return frase.length() > TAMANHO_DESCRICAO
                ? frase.substring(0, TAMANHO_DESCRICAO - 1).trim() + "…"
                : frase;
    }

    /** "warning" quando a IA apontou pontos de atenção, senão "ok". */
    public static String visualStatus(JsonNode analise) {
        return analise.path("pontosDeAtencao").isEmpty() ? "ok" : "warning";
    }

    /** Pontos de atenção primeiro (severity warning), depois os demais itens (severity info). */
    public static List<PontoChave> pontosChave(JsonNode analise) {
        List<PontoChave> pontos = new ArrayList<>();

        for (JsonNode item : analise.path("pontosDeAtencao")) {
            adicionar(pontos, "Ponto de atenção", texto(item), "warning");
        }
        for (JsonNode item : analise.path("valores")) {
            adicionar(pontos, texto(item.path("descricao")), texto(item.path("valor")), "info", "Valor");
        }
        for (JsonNode item : analise.path("prazos")) {
            adicionar(pontos, texto(item.path("descricao")), texto(item.path("prazo")), "info", "Prazo");
        }
        for (JsonNode item : analise.path("multasEJuros")) {
            adicionar(pontos, texto(item.path("descricao")), texto(item.path("detalhe")), "info", "Multa ou juros");
        }
        for (JsonNode item : analise.path("obrigacoes")) {
            adicionar(pontos, "Obrigação", texto(item), "info");
        }
        return pontos;
    }

    private static void adicionar(List<PontoChave> pontos, String titulo, String descricao, String severidade) {
        adicionar(pontos, titulo, descricao, severidade, titulo);
    }

    // Itens sem descrição não têm o que mostrar e são ignorados; título ausente usa o padrão.
    private static void adicionar(List<PontoChave> pontos, String titulo, String descricao,
                                  String severidade, String tituloPadrao) {
        if (descricao == null) {
            return;
        }
        pontos.add(new PontoChave("kp-" + (pontos.size() + 1),
                titulo != null ? titulo : tituloPadrao, descricao, severidade));
    }

    private static String texto(JsonNode no) {
        if (no == null || no.isMissingNode() || no.isNull()) {
            return null;
        }
        String valor = no.asText("").trim();
        return valor.isEmpty() ? null : valor;
    }
}
