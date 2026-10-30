package com.kaup.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kaup.exception.MuitasTentativasException;

class LimiteDeTentativasTest {

    /** Relógio que só anda quando o teste manda. */
    private static class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-01-01T12:00:00Z");

        void avancar(Duration d) { agora = agora.plus(d); }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private RelogioManual relogio;
    private LimiteDeTentativas limite;

    @BeforeEach
    void preparar() {
        relogio = new RelogioManual();
        limite = new LimiteDeTentativas(relogio);
    }

    private void errar(String chave, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limite.verificar(chave);
            limite.falhou(chave);
        }
    }

    @Test
    void quatroErrosAindaNaoBloqueiam() {
        errar("ana", 4);

        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void quintoErroBloqueiaPorNoMaximoDezSegundos() {
        errar("ana", 5);

        MuitasTentativasException e = assertThrows(MuitasTentativasException.class, () -> limite.verificar("ana"));
        assertTrue(e.getSegundos() >= 1 && e.getSegundos() <= 10, "espera de " + e.getSegundos() + " s");
    }

    @Test
    void depoisDeDezSegundosLiberaENaoBloqueiaMaisPorEsseMotivo() {
        errar("ana", 5);
        relogio.avancar(Duration.ofSeconds(9));
        assertThrows(MuitasTentativasException.class, () -> limite.verificar("ana"));

        relogio.avancar(Duration.ofSeconds(1));

        assertDoesNotThrow(() -> limite.verificar("ana"));
        // a contagem recomeçou: 4 erros novos ainda não bloqueiam
        errar("ana", 4);
        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void persistirNosErrosBloqueiaDeNovoMasSempreSoPorDezSegundos() {
        for (int rodada = 0; rodada < 3; rodada++) {
            errar("ana", 5);
            MuitasTentativasException e = assertThrows(MuitasTentativasException.class, () -> limite.verificar("ana"));
            assertEquals(10, e.getSegundos());
            relogio.avancar(Duration.ofSeconds(10));
            assertDoesNotThrow(() -> limite.verificar("ana"));
        }
    }

    @Test
    void acertarZeraOsErros() {
        errar("ana", 4);
        limite.acertou("ana");
        errar("ana", 4);

        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void errosEspacadosAlemDaJanelaNaoSeSomam() {
        errar("ana", 4);
        relogio.avancar(Duration.ofMinutes(2));
        errar("ana", 4);

        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void ObloqueioDeUmaChaveNaoAfetaAsOutras() {
        errar("ana", 5);

        assertThrows(MuitasTentativasException.class, () -> limite.verificar("ana"));
        assertDoesNotThrow(() -> limite.verificar("bia"));
    }

    @Test
    void chavesAntigasSaoDescartadasParaNaoEncherAMemoria() throws Exception {
        for (int i = 0; i < 1000; i++) {
            limite.falhou("spam" + i);
        }
        relogio.avancar(Duration.ofMinutes(3));
        limite.falhou("outra"); // dispara a limpeza

        var campo = LimiteDeTentativas.class.getDeclaredField("estados");
        campo.setAccessible(true);
        assertEquals(1, ((java.util.Map<?, ?>) campo.get(limite)).size());
    }
}
