package com.kaup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import com.kaup.dto.ExportacaoResponse;
import com.kaup.exception.OperacaoNaoPermitidaException;
import com.kaup.security.WithUsuarioLogado;
import com.kaup.security.WithUsuarioLogadoFactory;
import com.kaup.service.ContaService;
import com.kaup.service.ExportacaoService;

@WebMvcTest(ContaController.class)
@WithUsuarioLogado
class ContaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContaService contaService;

    @MockBean
    private ExportacaoService exportacao;

    @Test
    void exportarBaixaOsDadosDeQuemEstaLogadoComoArquivo() throws Exception {
        ExportacaoResponse.Preferencias prefs =
                new ExportacaoResponse.Preferencias("large", "simple", "prefer_not", false, true);
        when(exportacao.exportar(WithUsuarioLogadoFactory.ID)).thenReturn(new ExportacaoResponse("2026-01-01T00:00:00Z",
                new ExportacaoResponse.Conta("Ana", "ana@exemplo.com", "USER", "2026-01-01T00:00:00Z", prefs), List.of()));

        mockMvc.perform(get("/api/conta/exportar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("kaup-meus-dados.json")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.account.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.account.preferences.fontSize").value("large"))
                .andExpect(jsonPath("$.documents").isEmpty());
    }

    @Test
    void exportarSemLoginRetorna401() throws Exception {
        mockMvc.perform(get("/api/conta/exportar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isUnauthorized());
    }

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
