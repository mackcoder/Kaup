package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kaup.repository.DocumentoRepository;

class RetencaoTest {

    private final DocumentoRepository documentos = mock(DocumentoRepository.class);

    @Test
    void apagaOsDocumentosMaisAntigosQueOPrazo() {
        when(documentos.apagarDeUsuariosAnterioresA(any())).thenReturn(3);
        ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);

        int apagados = new Retencao(documentos, 180).apagarVencidos();

        assertEquals(3, apagados);
        verify(documentos).apagarDeUsuariosAnterioresA(limite.capture());
        Instant esperado = Instant.now().minus(Duration.ofDays(180));
        assertTrue(Duration.between(limite.getValue(), esperado).abs().toSeconds() < 5);
    }

    @Test
    void zeroOuNegativoDesligaAExpiracao() {
        assertEquals(0, new Retencao(documentos, 0).apagarVencidos());
        assertEquals(0, new Retencao(documentos, -5).apagarVencidos());
        assertEquals(0, new Retencao(documentos, -5).dias());

        verify(documentos, never()).apagarDeUsuariosAnterioresA(any());
    }

    @Test
    void informaOPrazoParaOPerfil() {
        assertEquals(90, new Retencao(documentos, 90).dias());
    }
}
