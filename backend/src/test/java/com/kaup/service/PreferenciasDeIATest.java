package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kaup.entity.Usuario;

class PreferenciasDeIATest {

    private Usuario usuario(String nivel, String renda, boolean compartilhar) {
        Usuario u = new Usuario();
        u.setExplainLevel(nivel);
        u.setIncome(renda);
        u.setCompartilharRenda(compartilhar);
        u.setNome("Ana Souza");
        u.setEmail("ana@exemplo.com");
        return u;
    }

    @Test
    void rendaInformadaEPermitidaVaiComoFaixaLegivel() {
        PreferenciasDeIA prefs = PreferenciasDeIA.de(usuario("normal", "3to5", true));

        assertEquals("normal", prefs.nivelDeExplicacao());
        assertEquals("entre R$ 3.000 e R$ 5.000", prefs.faixaDeRenda());
        assertTrue(prefs.instrucaoDeRenda().contains("entre R$ 3.000 e R$ 5.000"));
    }

    @Test
    void bloqueioDoUsuarioImpedeOEnvioMesmoComARendaPreenchida() {
        PreferenciasDeIA prefs = PreferenciasDeIA.de(usuario("simple", "more15", false));

        assertNull(prefs.faixaDeRenda());
        // A IA fica sabendo que não tem a renda, mas nunca recebe o valor.
        assertTrue(prefs.instrucaoDeRenda().contains("não tem a renda"));
        assertFalse(prefs.instrucaoDeRenda().contains("R$"));
        assertFalse(prefs.instrucaoDeRenda().contains("more15"));
    }

    @Test
    void preferirNaoInformarNuncaEnviaRenda() {
        PreferenciasDeIA prefs = PreferenciasDeIA.de(usuario("simple", "prefer_not", true));

        assertNull(prefs.faixaDeRenda());
        assertTrue(prefs.instrucaoDeRenda().contains("não tem a renda"));
    }

    @Test
    void comARendaDisponivelAIaPodeDizerlaQuandoPerguntada() {
        PreferenciasDeIA prefs = PreferenciasDeIA.de(usuario("simple", "7to15", true));

        assertTrue(prefs.instrucaoDeRenda().contains("entre R$ 7.000 e R$ 15.000"));
        assertTrue(prefs.instrucaoDeRenda().contains("diga a faixa"));
        // Não deve virar assunto em respostas que não têm relação com renda.
        assertTrue(prefs.instrucaoDeRenda().contains("não a traga em respostas"));
    }

    @Test
    void cadaNivelDeExplicacaoTemInstrucaoPropria() {
        assertTrue(PreferenciasDeIA.de(usuario("simple", "prefer_not", true)).instrucaoDeExplicacao().contains("SIMPLES"));
        assertTrue(PreferenciasDeIA.de(usuario("normal", "prefer_not", true)).instrucaoDeExplicacao().contains("NORMAL"));
        assertTrue(PreferenciasDeIA.de(usuario("complete", "prefer_not", true)).instrucaoDeExplicacao().contains("COMPLETO"));
    }

    @Test
    void nomeEEmailNuncaEntramNasInstrucoes() {
        PreferenciasDeIA prefs = PreferenciasDeIA.de(usuario("complete", "5to7", true));
        String tudo = prefs.instrucaoDeExplicacao() + prefs.instrucaoDeRenda() + prefs;

        assertTrue(!tudo.contains("Ana Souza") && !tudo.contains("ana@exemplo.com"));
    }
}
