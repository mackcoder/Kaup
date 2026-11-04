package com.kaup.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kaup.exception.LimiteDiarioException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.MensagemChatRepository;

/**
 * Teto <b>global</b> por dia de chamadas ao Gemini, para uma chave só não estourar a cota. Não limita
 * ninguém em particular: soma os contratos analisados e as perguntas feitas hoje (por todos) e, quando
 * passa do teto, recusa novos pedidos até a meia-noite.
 *
 * <p>A contagem vem das tabelas {@code documento} e {@code mensagem_chat}, sem tabela própria, então
 * sobrevive a reinícios. É uma aproximação: análises que falharam não deixam rastro, e documentos
 * apagados no mesmo dia deixam de contar. Basta para proteger a cota.
 */
@Service
public class TetoDiarioGemini {

    private final DocumentoRepository documentos;
    private final MensagemChatRepository mensagens;
    private final int teto;
    private final ZoneId fuso;
    private final Clock relogio;

    @Autowired
    public TetoDiarioGemini(DocumentoRepository documentos, MensagemChatRepository mensagens,
                            @Value("${kaup.teto-diario-gemini}") int teto,
                            @Value("${kaup.fuso}") String fuso) {
        this(documentos, mensagens, teto, ZoneId.of(fuso), Clock.systemUTC());
    }

    TetoDiarioGemini(DocumentoRepository documentos, MensagemChatRepository mensagens, int teto,
                     ZoneId fuso, Clock relogio) {
        this.documentos = documentos;
        this.mensagens = mensagens;
        this.teto = teto;
        this.fuso = fuso;
        this.relogio = relogio;
    }

    /** Lança {@link LimiteDiarioException} se o teto de hoje já foi atingido. 0 desliga o teto. */
    public void verificar() {
        if (teto <= 0) {
            return;
        }
        Instant agora = relogio.instant();
        ZonedDateTime inicioDoDia = agora.atZone(fuso).toLocalDate().atStartOfDay(fuso);
        Instant desde = inicioDoDia.toInstant();
        long usadas = documentos.countByCriadoEmGreaterThanEqual(desde)
                + mensagens.countByPapelAndCriadoEmGreaterThanEqual(TurnoDeConversa.USUARIO, desde);
        if (usadas >= teto) {
            long restante = Duration.between(agora, inicioDoDia.plusDays(1).toInstant()).toSeconds();
            throw new LimiteDiarioException(Math.max(1, restante));
        }
    }
}
