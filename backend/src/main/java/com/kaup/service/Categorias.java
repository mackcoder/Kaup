package com.kaup.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Categorias de documento. As chaves são as que o frontend conhece
 * (frontend/src/styles/categories.ts); as descrições orientam a IA ao classificar.
 */
public final class Categorias {

    public static final String OUTROS = "Outros";

    /** chave -> descrição, na ordem de exibição. */
    public static final Map<String, String> DESCRICOES = new LinkedHashMap<>();

    static {
        DESCRICOES.put("Carros", "veículos: compra, financiamento, aluguel ou seguro de carro ou moto");
        DESCRICOES.put("Casa", "moradia: aluguel, compra ou financiamento de imóvel, condomínio");
        DESCRICOES.put("Trabalho", "emprego, estágio ou prestação de serviços");
        DESCRICOES.put("Banco", "serviços financeiros: empréstimo, cartão, conta, investimento");
        DESCRICOES.put(OUTROS, "qualquer outro assunto");
    }

    private Categorias() { }

    /**
     * Devolve a chave oficial equivalente ao texto (ignorando maiúsculas e espaços nas pontas),
     * ou {@code null} se o texto não corresponde a nenhuma categoria.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String procurado = texto.trim();
        return DESCRICOES.keySet().stream()
                .filter(chave -> chave.equalsIgnoreCase(procurado))
                .findFirst()
                .orElse(null);
    }
}
