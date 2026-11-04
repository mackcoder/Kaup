package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kaup.exception.LimiteDiarioException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.MensagemChatRepository;

class TetoDiarioGeminiTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    // 15h em Brasília (UTC-3): o dia local começou às 03:00 UTC e acaba em 9 h.
    private static final Instant AGORA = Instant.parse("2026-06-10T18:00:00Z");
    private static final Instant INICIO_DO_DIA = Instant.parse("2026-06-10T03:00:00Z");

    private DocumentoRepository documentos;
    private MensagemChatRepository mensagens;

    @BeforeEach
    void preparar() {
        documentos = mock(DocumentoRepository.class);
        mensagens = mock(MensagemChatRepository.class);
    }

    private TetoDiarioGemini teto(int limite) {
        return new TetoDiarioGemini(documentos, mensagens, limite, FUSO, Clock.fixed(AGORA, ZoneId.of("UTC")));
    }

    private void uso(long contratos, long perguntas) {
        when(documentos.countByCriadoEmGreaterThanEqual(INICIO_DO_DIA)).thenReturn(contratos);
        when(mensagens.countByPapelAndCriadoEmGreaterThanEqual(eq("user"), eq(INICIO_DO_DIA))).thenReturn(perguntas);
    }

    @Test
    void abaixoDoTetoPassa() {
        uso(3, 496);
        assertDoesNotThrow(() -> teto(500).verificar());
    }

    @Test
    void somaContratosEPerguntasENoTetoRecusaComOTempoAteAMeiaNoite() {
        uso(10, 490);
        LimiteDiarioException e = assertThrows(LimiteDiarioException.class, () -> teto(500).verificar());
        assertEquals("O Kaup atingiu o limite de uso de hoje. Tente amanhã.", e.getMessage());
        assertEquals(9 * 3600, e.getSegundos());
    }

    @Test
    void zeroDesligaOTetoSemNemConsultarOBanco() {
        assertDoesNotThrow(() -> teto(0).verificar());
        verifyNoInteractions(documentos, mensagens);
    }

    @Test
    void contaSoDesdeOComecoDoDiaNoFusoConfigurado() {
        uso(0, 0);
        teto(1).verificar();
        // Se o dia fosse contado em UTC (00:00Z), a consulta usaria outro instante e este stub não casaria.
        when(documentos.countByCriadoEmGreaterThanEqual(Instant.parse("2026-06-10T00:00:00Z"))).thenReturn(99L);
        assertDoesNotThrow(() -> teto(1).verificar());
    }
}
