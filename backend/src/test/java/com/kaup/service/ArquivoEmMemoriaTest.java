package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArquivoEmMemoriaTest {

    @Test
    void guardaOsBytesONomeEOTipoDoOriginal() throws Exception {
        byte[] bytes = "conteudo".getBytes();
        ArquivoEmMemoria arquivo = new ArquivoEmMemoria("c.pdf", "application/pdf", bytes);

        assertEquals("c.pdf", arquivo.getOriginalFilename());
        assertEquals("application/pdf", arquivo.getContentType());
        assertEquals(bytes.length, arquivo.getSize());
        assertFalse(arquivo.isEmpty());
        assertArrayEquals(bytes, arquivo.getBytes());
        assertArrayEquals(bytes, arquivo.getInputStream().readAllBytes());
    }

    @Test
    void arquivoSemBytesEstaVazio() {
        assertTrue(new ArquivoEmMemoria("c.pdf", "application/pdf", new byte[0]).isEmpty());
    }

    @Test
    void transferToGravaOArquivo(@TempDir File pasta) throws Exception {
        File destino = new File(pasta, "saida.bin");

        new ArquivoEmMemoria("c.pdf", "application/pdf", "abc".getBytes()).transferTo(destino);

        assertEquals("abc", Files.readString(destino.toPath()));
    }
}
