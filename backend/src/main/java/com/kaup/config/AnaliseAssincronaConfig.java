package com.kaup.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor da leitura dos contratos (OCR + IA), fora das threads do Tomcat. Poucas threads e fila
 * curta de propósito: com a fila cheia o envio é recusado (429), o que também protege o servidor
 * de uma enxurrada de envios.
 */
@Configuration
public class AnaliseAssincronaConfig {

    @Bean(name = "executorAnalise", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor executorAnalise(@Value("${kaup.analise.threads}") int threads,
                                                  @Value("${kaup.analise.fila}") int fila) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(fila);
        executor.setThreadNamePrefix("analise-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
