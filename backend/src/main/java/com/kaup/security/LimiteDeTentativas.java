package com.kaup.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.kaup.exception.MuitasTentativasException;

/**
 * Freia quem erra a senha (ou o código de recuperação) várias vezes seguidas. Depois de
 * {@link #MAXIMO_DE_ERROS} erros dentro de {@link #JANELA}, a chave (o e-mail) fica bloqueada por
 * {@link #BLOQUEIO}, e só isso: o bloqueio nunca é permanente nem passa de 10 segundos.
 *
 * <p>Fica em memória, sem tabela: reiniciar o backend zera os contadores, o que só ajuda quem
 * está bloqueado.
 */
@Component
public class LimiteDeTentativas {

    static final int MAXIMO_DE_ERROS = 5;
    static final Duration BLOQUEIO = Duration.ofSeconds(10);
    // Erros mais espaçados que isso não se somam.
    static final Duration JANELA = Duration.ofMinutes(1);

    private static class Estado {
        int erros;
        Instant ultimoErro = Instant.MIN;
        Instant bloqueadoAte = Instant.MIN;
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();
    private final Clock relogio;
    private Instant ultimaLimpeza;

    public LimiteDeTentativas() {
        this(Clock.systemUTC());
    }

    LimiteDeTentativas(Clock relogio) {
        this.relogio = relogio;
        this.ultimaLimpeza = relogio.instant();
    }

    /** Lança {@link MuitasTentativasException} se a chave está bloqueada neste momento. */
    public void verificar(String chave) {
        Instant agora = relogio.instant();
        Estado estado = estados.get(chave);
        if (estado == null) {
            return;
        }
        synchronized (estado) {
            if (agora.isBefore(estado.bloqueadoAte)) {
                long restante = Duration.between(agora, estado.bloqueadoAte).toMillis();
                throw new MuitasTentativasException(Math.max(1, (restante + 999) / 1000));
            }
        }
    }

    /** Conta um erro; no quinto seguido, bloqueia por 10 segundos e recomeça a contagem. */
    public void falhou(String chave) {
        Instant agora = relogio.instant();
        limparVelhos(agora);
        Estado estado = estados.computeIfAbsent(chave, k -> new Estado());
        synchronized (estado) {
            if (agora.isAfter(estado.ultimoErro.plus(JANELA))) {
                estado.erros = 0;
            }
            estado.erros++;
            estado.ultimoErro = agora;
            if (estado.erros >= MAXIMO_DE_ERROS) {
                estado.bloqueadoAte = agora.plus(BLOQUEIO);
                estado.erros = 0;
            }
        }
    }

    /** Acertou: esquece os erros anteriores. */
    public void acertou(String chave) {
        estados.remove(chave);
    }

    // Quem erra com milhares de e-mails diferentes não pode encher a memória: de minuto em
    // minuto, descarta as chaves sem erro recente e sem bloqueio ativo.
    private synchronized void limparVelhos(Instant agora) {
        if (agora.isBefore(ultimaLimpeza.plus(JANELA))) {
            return;
        }
        ultimaLimpeza = agora;
        estados.entrySet().removeIf(e -> {
            Estado s = e.getValue();
            synchronized (s) {
                return agora.isAfter(s.ultimoErro.plus(JANELA)) && !agora.isBefore(s.bloqueadoAte);
            }
        });
    }
}
