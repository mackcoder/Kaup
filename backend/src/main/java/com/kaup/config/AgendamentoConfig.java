package com.kaup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga as tarefas agendadas (hoje, só a retenção de documentos). */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
