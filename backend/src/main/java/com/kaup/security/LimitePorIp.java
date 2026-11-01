package com.kaup.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de requisições por endereço IP, para a API não cair com um laço de {@code curl}. Três
 * contadores de janela fixa, cada um desligado quando o limite é 0:
 * <ul>
 *   <li>geral: toda requisição, por minuto;
 *   <li>login: {@code POST /api/auth/*} (entrar, cadastro, recuperação), por minuto;
 *   <li>cadastro: {@code POST /api/auth/cadastro}, por hora, para não criar mil contas.
 * </ul>
 *
 * <p>Fica em memória, como o {@link LimiteDeTentativas}: reiniciar o backend zera os contadores.
 */
public class LimitePorIp {

    static final Duration MINUTO = Duration.ofMinutes(1);
    static final Duration HORA = Duration.ofHours(1);
    // Teto de endereços guardados por contador: um ataque com milhões de IPs não enche a memória.
    static final int MAXIMO_DE_ENDERECOS = 100_000;

    private static class Janela {
        Instant inicio;
        int usos;
    }

    private class Contador {
        final int limite;
        final Duration duracao;
        final Map<String, Janela> janelas = new ConcurrentHashMap<>();
        Instant ultimaLimpeza;

        Contador(int limite, Duration duracao) {
            this.limite = limite;
            this.duracao = duracao;
            this.ultimaLimpeza = relogio.instant();
        }

        /** Conta um uso; se passou do limite, devolve quantos segundos faltam para a janela virar. */
        OptionalLong usar(String ip, Instant agora) {
            if (limite <= 0) {
                return OptionalLong.empty();
            }
            limparVelhas(agora);
            Janela janela = janelas.computeIfAbsent(ip, k -> new Janela());
            synchronized (janela) {
                if (janela.inicio == null || !agora.isBefore(janela.inicio.plus(duracao))) {
                    janela.inicio = agora;
                    janela.usos = 0;
                }
                janela.usos++;
                if (janela.usos <= limite) {
                    return OptionalLong.empty();
                }
                long restante = Duration.between(agora, janela.inicio.plus(duracao)).toMillis();
                return OptionalLong.of(Math.max(1, (restante + 999) / 1000));
            }
        }

        private synchronized void limparVelhas(Instant agora) {
            if (agora.isBefore(ultimaLimpeza.plus(duracao)) && janelas.size() < MAXIMO_DE_ENDERECOS) {
                return;
            }
            ultimaLimpeza = agora;
            janelas.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return !agora.isBefore(e.getValue().inicio.plus(duracao));
                }
            });
            if (janelas.size() >= MAXIMO_DE_ENDERECOS) {
                janelas.clear();
            }
        }
    }

    private final Clock relogio;
    private final Contador geral;
    private final Contador login;
    private final Contador cadastro;

    public LimitePorIp(int porMinuto, int loginsPorMinuto, int cadastrosPorHora) {
        this(porMinuto, loginsPorMinuto, cadastrosPorHora, Clock.systemUTC());
    }

    LimitePorIp(int porMinuto, int loginsPorMinuto, int cadastrosPorHora, Clock relogio) {
        this.relogio = relogio;
        this.geral = new Contador(porMinuto, MINUTO);
        this.login = new Contador(loginsPorMinuto, MINUTO);
        this.cadastro = new Contador(cadastrosPorHora, HORA);
    }

    /** Resultado de {@link #verificar}: quantos segundos esperar e qual regra estourou. */
    public record Bloqueio(long segundos, boolean cadastro) { }

    /** Conta a requisição; devolve o bloqueio se algum limite foi ultrapassado, ou {@code null}. */
    public Bloqueio verificar(String ip, String metodo, String caminho) {
        Instant agora = relogio.instant();
        Bloqueio bloqueio = null;

        OptionalLong espera = geral.usar(ip, agora);
        if (espera.isPresent()) {
            bloqueio = new Bloqueio(espera.getAsLong(), false);
        }
        if ("POST".equals(metodo) && caminho.startsWith("/api/auth/")) {
            espera = login.usar(ip, agora);
            if (espera.isPresent() && bloqueio == null) {
                bloqueio = new Bloqueio(espera.getAsLong(), false);
            }
            if ("/api/auth/cadastro".equals(caminho)) {
                espera = cadastro.usar(ip, agora);
                if (espera.isPresent()) {
                    bloqueio = new Bloqueio(espera.getAsLong(), true);
                }
            }
        }
        return bloqueio;
    }
}
