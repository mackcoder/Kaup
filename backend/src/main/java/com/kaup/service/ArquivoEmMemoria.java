package com.kaup.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.springframework.web.multipart.MultipartFile;

/**
 * Cópia em memória do arquivo enviado. O {@code MultipartFile} da requisição pode ter o arquivo
 * temporário apagado assim que a resposta sai, e a leitura agora roda depois disso.
 */
final class ArquivoEmMemoria implements MultipartFile {

    private final String nome;
    private final String tipo;
    private final byte[] bytes;

    ArquivoEmMemoria(String nome, String tipo, byte[] bytes) {
        this.nome = nome;
        this.tipo = tipo;
        this.bytes = bytes;
    }

    @Override public String getName() { return "file"; }
    @Override public String getOriginalFilename() { return nome; }
    @Override public String getContentType() { return tipo; }
    @Override public boolean isEmpty() { return bytes.length == 0; }
    @Override public long getSize() { return bytes.length; }
    @Override public byte[] getBytes() { return bytes; }
    @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

    @Override
    public void transferTo(File destino) throws IOException {
        Files.write(destino.toPath(), bytes);
    }
}
