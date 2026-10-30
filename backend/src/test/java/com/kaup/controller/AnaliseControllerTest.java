package com.kaup.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.kaup.service.AnaliseService;
import com.kaup.service.OcrService;
import com.kaup.service.ServicoDeIA;

@WebMvcTest(AnaliseController.class)
@Import(AnaliseService.class)
@WithMockUser
class AnaliseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OcrService ocrService;

    @MockBean
    private ServicoDeIA servicoDeIA;

    @Test
    void healthRetornaOk() throws Exception {
        mockMvc.perform(get("/api/analise/health"))
                .andExpect(status().isOk());
    }

    @Test
    void textoVazioRetorna400() throws Exception {
        mockMvc.perform(post("/api/analise/texto").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void formatoNaoSuportadoRetorna400() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "contrato.txt", "text/plain", "conteudo".getBytes());
        mockMvc.perform(multipart("/api/analise/contrato").file(arquivo).with(csrf()))
                .andExpect(status().isBadRequest());
    }
}