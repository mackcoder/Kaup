package com.kaup.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.kaup.dto.PerfilResponse;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.EmailJaCadastradoException;
import com.kaup.security.WithUsuarioLogado;
import com.kaup.security.WithUsuarioLogadoFactory;
import com.kaup.service.PerfilService;

@WebMvcTest(PerfilController.class)
@WithUsuarioLogado
class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PerfilService perfilService;

    private PerfilResponse perfil() {
        return new PerfilResponse("Ana Souza", "ana@exemplo.com", "large", "simple", "prefer_not", false, true, 180);
    }

    @Test
    void getDevolveOPerfilDeQuemEstaLogadoNoFormatoDoFrontend() throws Exception {
        when(perfilService.obter(WithUsuarioLogadoFactory.ID)).thenReturn(perfil());

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.fontSize").value("large"))
                .andExpect(jsonPath("$.explainLevel").value("simple"))
                .andExpect(jsonPath("$.income").value("prefer_not"))
                .andExpect(jsonPath("$.audio").value(false))
                .andExpect(jsonPath("$.shareIncomeWithAi").value(true))
                .andExpect(jsonPath("$.retentionDays").value(180))
                .andExpect(jsonPath("$", hasKey("email")));
    }

    @Test
    void patchRepassaOCorpoESoMexeNoPerfilDeQuemEstaLogado() throws Exception {
        when(perfilService.atualizar(eq(WithUsuarioLogadoFactory.ID), any())).thenReturn(perfil());

        mockMvc.perform(patch("/api/perfil").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"explainLevel\":\"complete\",\"shareIncomeWithAi\":false}"))
                .andExpect(status().isOk());

        verify(perfilService).atualizar(eq(WithUsuarioLogadoFactory.ID),
                org.mockito.ArgumentMatchers.argThat(p -> "complete".equals(p.explainLevel())
                        && Boolean.FALSE.equals(p.shareIncomeWithAi()) && p.name() == null));
    }

    @Test
    void valorInvalidoRetorna400EEmailRepetidoRetorna409() throws Exception {
        when(perfilService.atualizar(any(), any()))
                .thenThrow(new DadosInvalidosException("Valor inválido para nível de explicação."))
                .thenThrow(new EmailJaCadastradoException());

        mockMvc.perform(patch("/api/perfil").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para nível de explicação."));
        mockMvc.perform(patch("/api/perfil").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void patchSemTokenCsrfEhRecusado() throws Exception {
        mockMvc.perform(patch("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
