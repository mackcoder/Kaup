package com.kaup.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.kaup.repository.DocumentoRepository;

/**
 * A fila de leitura fica na memória (os bytes do arquivo não vão para o banco). Se o servidor
 * reiniciou no meio, quem estava "pending" ou "processing" nunca terminaria: na subida, marca
 * todos como falhos para a pessoa poder enviar de novo.
 */
@Component
public class RecuperacaoDeEnviosInterrompidos {

    private static final Logger log = LoggerFactory.getLogger(RecuperacaoDeEnviosInterrompidos.class);

    private final DocumentoRepository repository;

    public RecuperacaoDeEnviosInterrompidos(DocumentoRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoSubir() {
        int total = repository.falharEmAndamento(ProcessadorDeDocumento.FALHA_INTERROMPIDO);
        if (total > 0) {
            log.warn("{} documento(s) interrompido(s) pelo reinício foram marcados como falhos", total);
        }
    }
}
