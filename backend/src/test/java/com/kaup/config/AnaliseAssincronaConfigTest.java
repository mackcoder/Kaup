package com.kaup.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Usa o executor de verdade: é ele que decide quando o envio recebe 429. */
class AnaliseAssincronaConfigTest {

    private ThreadPoolTaskExecutor executor(int threads, int fila) {
        ThreadPoolTaskExecutor executor = new AnaliseAssincronaConfig().executorAnalise(threads, fila);
        executor.initialize();
        return executor;
    }

    @Test
    void comAsThreadsOcupadasEAFilaCheiaRecusaOEnvio() throws Exception {
        ThreadPoolTaskExecutor executor = executor(1, 1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch comecou = new CountDownLatch(1);
        try {
            executor.execute(() -> { // ocupa a única thread
                comecou.countDown();
                await(liberar);
            });
            assertTrue(comecou.await(5, TimeUnit.SECONDS));
            executor.execute(() -> { }); // ocupa a única vaga da fila

            assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> { }));
        } finally {
            liberar.countDown();
            executor.shutdown();
        }
    }

    @Test
    void liberadaAThreadAFilaAndaETodasAsTarefasRodam() throws Exception {
        ThreadPoolTaskExecutor executor = executor(1, 5);
        AtomicInteger feitas = new AtomicInteger();
        CountDownLatch todas = new CountDownLatch(3);
        try {
            for (int i = 0; i < 3; i++) {
                executor.execute(() -> {
                    feitas.incrementAndGet();
                    todas.countDown();
                });
            }
            assertTrue(todas.await(5, TimeUnit.SECONDS));
            assertEquals(3, feitas.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void asThreadsTemNomeParaAparecerNosLogs() throws Exception {
        ThreadPoolTaskExecutor executor = executor(1, 1);
        StringBuilder nome = new StringBuilder();
        CountDownLatch fim = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                nome.append(Thread.currentThread().getName());
                fim.countDown();
            });
            assertTrue(fim.await(5, TimeUnit.SECONDS));
            assertTrue(nome.toString().startsWith("analise-"));
        } finally {
            executor.shutdown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
