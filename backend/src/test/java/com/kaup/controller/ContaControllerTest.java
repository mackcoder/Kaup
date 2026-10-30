package com.kaup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kaup.exception.OperacaoNaoPermitidaException;
import com.kaup.security.WithUsuarioLogado;
import com.kaup.service.ContaService;

@WebMvcTest(ContaController.class)
@WithUsuarioLogado
class ContaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContaService contaService;

    @Test
    void excluirContaRetorna204() throws Exception {
        mockMvc.perform(delete("/api/conta").with(csrf())).andExpect(status().isNoContent());

        verify(contaService).excluir(any());
    }

    @Test
    void administradorNaoPodeSerExcluidoRetorna403() throws Exception {
        doThrow(new OperacaoNaoPermitidaException("A conta de administrador não pode ser excluída."))
                .when(contaService).excluir(any());

        mockMvc.perform(delete("/api/conta").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("A conta de administrador não pode ser excluída."));
    }

    @Test
    void semTokenCsrfEhRecusado() throws Exception {
        mockMvc.perform(delete("/api/conta")).andExpect(status().isForbidden());
    }
}
