package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaup.dto.AnaliseResponse.PontoChave;

class AnaliseMapperTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String texto) throws Exception {
        return mapper.readTree(texto);
    }

    @Test
    void traduzTodosOsBlocosNaOrdemEsperada() throws Exception {
        JsonNode analise = json("""
                {
                  "resumo": "Contrato de aluguel. Define o valor e o prazo.",
                  "valores": [{"descricao": "Aluguel mensal", "valor": "R$ 1.500,00"}],
                  "prazos": [{"descricao": "Vencimento", "prazo": "Todo dia 5"}],
                  "multasEJuros": [{"descricao": "Atraso", "detalhe": "10% e 1% ao mês"}],
                  "obrigacoes": ["Pagar o condomínio"],
                  "pontosDeAtencao": ["Multa de três aluguéis na rescisão"]
                }""");

        List<PontoChave> pontos = AnaliseMapper.pontosChave(analise);

        assertEquals(5, pontos.size());
        assertEquals(new PontoChave("kp-1", "Ponto de atenção", "Multa de três aluguéis na rescisão", "warning"), pontos.get(0));
        assertEquals(new PontoChave("kp-2", "Aluguel mensal", "R$ 1.500,00", "info"), pontos.get(1));
        assertEquals(new PontoChave("kp-3", "Vencimento", "Todo dia 5", "info"), pontos.get(2));
        assertEquals(new PontoChave("kp-4", "Atraso", "10% e 1% ao mês", "info"), pontos.get(3));
        assertEquals(new PontoChave("kp-5", "Obrigação", "Pagar o condomínio", "info"), pontos.get(4));
        assertEquals("Contrato de aluguel. Define o valor e o prazo.", AnaliseMapper.resumo(analise));
        assertEquals("warning", AnaliseMapper.visualStatus(analise));
    }

    @Test
    void descricaoUsaSoAPrimeiraFrase() throws Exception {
        JsonNode analise = json("{\"resumo\": \"Contrato de aluguel. Define o valor e o prazo.\"}");

        assertEquals("Contrato de aluguel.", AnaliseMapper.descricao(analise));
    }

    @Test
    void descricaoLongaEhTruncadaComReticencias() throws Exception {
        String longa = "palavra ".repeat(40).trim();
        JsonNode analise = mapper.createObjectNode().put("resumo", longa);

        String descricao = AnaliseMapper.descricao(analise);

        assertEquals(140, descricao.length());
        assertTrue(descricao.endsWith("…"));
    }

    @Test
    void jsonVazioNaoQuebraENaoInventaNada() throws Exception {
        JsonNode analise = json("{}");

        assertNull(AnaliseMapper.resumo(analise));
        assertNull(AnaliseMapper.descricao(analise));
        assertTrue(AnaliseMapper.pontosChave(analise).isEmpty());
        assertEquals("ok", AnaliseMapper.visualStatus(analise));
    }

    @Test
    void ignoraItensSemDescricaoEUsaTituloPadrao() throws Exception {
        JsonNode analise = json("""
                {
                  "valores": [{"descricao": "", "valor": "R$ 10,00"}, {"descricao": "Sem valor", "valor": ""}],
                  "obrigacoes": ["   "],
                  "pontosDeAtencao": []
                }""");

        List<PontoChave> pontos = AnaliseMapper.pontosChave(analise);

        assertEquals(1, pontos.size());
        assertEquals(new PontoChave("kp-1", "Valor", "R$ 10,00", "info"), pontos.get(0));
        assertEquals("ok", AnaliseMapper.visualStatus(analise));
    }
}
