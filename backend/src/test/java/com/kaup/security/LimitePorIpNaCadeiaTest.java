package com.kaup.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.kaup.controller.AnaliseController;
import com.kaup.service.AnaliseService;

/** O filtro está mesmo ligado na cadeia do Spring Security, antes dos demais. */
@WebMvcTest(AnaliseController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"kaup.limite.ip-por-minuto=3", "kaup.limite.saltos-de-proxy=1"})
class LimitePorIpNaCadeiaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnaliseService analiseService;

    @MockBean
    private UserDetailsService usuarios;

    @Test
    void quartaRequisicaoDoMesmoIpRecebe429EOutroIpPassa() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/analise/health").header("X-Forwarded-For", "203.0.113.7"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/analise/health").header("X-Forwarded-For", "203.0.113.7"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        mockMvc.perform(get("/api/analise/health").header("X-Forwarded-For", "203.0.113.8"))
                .andExpect(status().isOk());
    }
}
