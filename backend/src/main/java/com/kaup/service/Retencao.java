package com.kaup.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kaup.repository.DocumentoRepository;

/**
 * Política de retenção: documentos de usuários com mais de {@code kaup.retencao.dias} dias são
 * apagados, com as conversas (cascata do banco). Roda uma vez por dia. Documentos sem dono (de
 * antes das contas, à espera de serem adotados) e a conta em si não são tocados. Com 0 ou
 * negativo, nada expira.
 */
@Component
public class Retencao {

    private static final Logger log = LoggerFactory.getLogger(Retencao.class);

    private final DocumentoRepository documentos;
    private final int dias;

    public Retencao(DocumentoRepository documentos, @Value("${kaup.retencao.dias:180}") int dias) {
        this.documentos = documentos;
        this.dias = dias;
    }

    /** Prazo em dias, ou 0 se os documentos não expiram. */
    public int dias() {
        return Math.max(dias, 0);
    }

    @Scheduled(cron = "${kaup.retencao.cron:0 30 3 * * *}")
    @Transactional
    public int apagarVencidos() {
        if (dias() == 0) {
            return 0;
        }
        int apagados = documentos.apagarDeUsuariosAnterioresA(Instant.now().minus(Duration.ofDays(dias)));
        if (apagados > 0) {
            log.info("Retenção: {} documento(s) com mais de {} dias apagado(s).", apagados, dias);
        }
        return apagados;
    }
}
