package com.kaup.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LimitePorIpTest {

    private static class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-01-01T12:00:00Z");

        void avancar(Duration d) { agora = agora.plus(d); }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private RelogioManual relogio;
    private LimitePorIp limite;

    @BeforeEach
    void preparar() {
        relogio = new RelogioManual();
        limite = new LimitePorIp(60, 10, 5, relogio);
    }

    private LimitePorIp.Bloqueio get(String ip) {
        return limite.verificar(ip, "GET", "/api/documentos");
    }

    @Test
    void aceita60PorMinutoERecusaA61() {
        for (int i = 0; i < 60; i++) {
            assertNull(get("1.1.1.1"), "requisição " + (i + 1));
        }
        LimitePorIp.Bloqueio bloqueio = get("1.1.1.1");
        assertNotNull(bloqueio);
        assertFalse(bloqueio.cadastro());
        assertEquals(60, bloqueio.segundos());
    }

    @Test
    void cadaIpTemOSeuContador() {
        for (int i = 0; i < 61; i++) {
            get("1.1.1.1");
        }
        assertNull(get("2.2.2.2"));
    }

    @Test
    void depoisDeUmMinutoLibera() {
        for (int i = 0; i < 61; i++) {
            get("1.1.1.1");
        }
        relogio.avancar(Duration.ofSeconds(61));
        assertNull(get("1.1.1.1"));
    }

    @Test
    void retryAfterDiminuiComOTempo() {
        for (int i = 0; i < 61; i++) {
            get("1.1.1.1");
        }
        relogio.avancar(Duration.ofSeconds(45));
        assertEquals(15, get("1.1.1.1").segundos());
    }

    @Test
    void autenticacaoTemLimiteMenorSoParaPost() {
        for (int i = 0; i < 10; i++) {
            assertNull(limite.verificar("1.1.1.1", "POST", "/api/auth/entrar"));
        }
        assertNotNull(limite.verificar("1.1.1.1", "POST", "/api/auth/entrar"));
        // Consultar a sessão (GET /api/auth/eu) a cada página não conta como tentativa de login.
        assertNull(limite.verificar("1.1.1.1", "GET", "/api/auth/eu"));
    }

    @Test
    void cadastroLimitaA5PorHoraEAvisaQueEhCadastro() {
        for (int i = 0; i < 5; i++) {
            assertNull(limite.verificar("1.1.1.1", "POST", "/api/auth/cadastro"));
            relogio.avancar(Duration.ofSeconds(13)); // não estoura o limite de login (10/min)
        }
        LimitePorIp.Bloqueio bloqueio = limite.verificar("1.1.1.1", "POST", "/api/auth/cadastro");
        assertNotNull(bloqueio);
        assertTrue(bloqueio.cadastro());
        assertTrue(bloqueio.segundos() > 60);

        relogio.avancar(Duration.ofHours(1));
        assertNull(limite.verificar("1.1.1.1", "POST", "/api/auth/cadastro"));
    }

    @Test
    void zeroDesligaOLimite() {
        LimitePorIp livre = new LimitePorIp(0, 0, 0, relogio);
        for (int i = 0; i < 1000; i++) {
            assertNull(livre.verificar("1.1.1.1", "POST", "/api/auth/cadastro"));
        }
    }
}
