package com.kaup.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.kaup.repository.DocumentoRepository;

class RecuperacaoDeEnviosInterrompidosTest {

    @Test
    void naSubidaMarcaOQueEstavaEmAndamentoComoFalho() {
        DocumentoRepository repository = mock(DocumentoRepository.class);

        new RecuperacaoDeEnviosInterrompidos(repository).aoSubir();

        verify(repository).falharEmAndamento("O envio foi interrompido, envie de novo.");
    }
}
