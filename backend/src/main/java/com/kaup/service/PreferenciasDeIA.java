package com.kaup.service;

import java.util.Map;

import com.kaup.entity.Usuario;

/**
 * O que a IA fica sabendo sobre quem pergunta. Só o nível de explicação e, se a pessoa
 * permitir, a faixa de renda: nome e e-mail nunca vão para a IA.
 *
 * @param nivelDeExplicacao simple, normal ou complete
 * @param faixaDeRenda      descrição legível da faixa (ex.: "entre R$ 3.000 e R$ 5.000"), ou
 *                          nulo quando não foi informada ou a pessoa bloqueou o envio
 */
public record PreferenciasDeIA(String nivelDeExplicacao, String faixaDeRenda) {

    /** Sem perfil conhecido (rotas de depuração). */
    public static final PreferenciasDeIA PADRAO = new PreferenciasDeIA("simple", null);

    private static final Map<String, String> RENDA = Map.of(
            "less3", "menos de R$ 3.000",
            "3to5", "entre R$ 3.000 e R$ 5.000",
            "5to7", "entre R$ 5.000 e R$ 7.000",
            "7to15", "entre R$ 7.000 e R$ 15.000",
            "more15", "acima de R$ 15.000");

    public static PreferenciasDeIA de(Usuario usuario) {
        // O bloqueio vale mesmo que a renda esteja preenchida no perfil.
        String renda = usuario.isCompartilharRenda() ? RENDA.get(usuario.getIncome()) : null;
        return new PreferenciasDeIA(usuario.getExplainLevel(), renda);
    }

    /** Trecho de instrução sobre o estilo da explicação. */
    public String instrucaoDeExplicacao() {
        return switch (nivelDeExplicacao) {
            case "complete" -> "Nível de explicação: COMPLETO. Dê explicações detalhadas, cite as cláusulas e os "
                    + "trechos relevantes e explique as consequências de cada ponto.";
            case "normal" -> "Nível de explicação: NORMAL. Explique com clareza e use termos jurídicos comuns "
                    + "apenas quando necessário, explicando-os.";
            default -> "Nível de explicação: SIMPLES. Use frases curtas, palavras do dia a dia e exemplos práticos. "
                    + "Evite termos técnicos; se precisar de um, explique-o em uma frase.";
        };
    }

    /**
     * Trecho de instrução sobre a renda. Vale nos dois casos: com a renda disponível a IA a usa e
     * a diz quando perguntada; sem ela, a IA sabe que não a tem (em vez de procurá-la no contrato).
     */
    public String instrucaoDeRenda() {
        if (faixaDeRenda == null) {
            return "Você não tem a renda da pessoa (ela não informou ou preferiu não compartilhar). "
                    + "Se ela perguntar, diga com gentileza que você não tem essa informação e que ela "
                    + "pode contar na conversa se quiser.";
        }
        return "A pessoa informou que a renda mensal dela está na faixa de " + faixaDeRenda + ". Você pode "
                + "usar isso: se ela perguntar qual é a renda dela, diga a faixa; e use-a para dizer se valores "
                + "e parcelas do contrato pesam no orçamento, quando a pergunta for sobre isso. Só mencione a "
                + "renda nessas situações: não a traga em respostas que não tenham relação com renda ou "
                + "orçamento, nem diga que ela \"tem folga\" sem ter sido perguntado. Não peça mais dados "
                + "pessoais.";
    }
}
