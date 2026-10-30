package com.kaup.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.kaup.dto.AnexoResponse;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.security.WithUsuarioLogado;
import com.kaup.service.ContextoService;

@WebMvcTest(ContextoController.class)
@WithUsuarioLogado
class ContextoControllerTest {

    private static final String ID = "3f2a9c1e-5b7d-4c8a-9e0f-1a2b3c4d5e6f";
    private static final String URL = "/api/documentos/" + ID + "/contexto";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContextoService contextoService;

    private AnexoResponse anexo() {
        return new AnexoResponse("4", ID, "aditivo.pdf", "application/pdf", Instant.parse("2026-10-30T12:00:00Z"));
    }

    private MockMultipartFile arquivo() {
        return new MockMultipartFile("file", "aditivo.pdf", "application/pdf", "conteudo".getBytes());
    }

    @Test
    void anexarRetorna201ComIdEmTexto() throws Exception {
        when(contextoService.anexar(eq(ID), any(), any())).thenReturn(anexo());

        mockMvc.perform(multipart(URL).file(arquivo()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("4"))
                .andExpect(jsonPath("$.documentId").value(ID))
                .andExpect(jsonPath("$.name").value("aditivo.pdf"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-30T12:00:00Z"));
    }

    @Test
    void anexoInvalidoRetorna400() throws Exception {
        when(contextoService.anexar(eq(ID), any(), any()))
                .thenThrow(new ContratoInvalidoException("Formato não suportado. Envie PDF, PNG ou JPEG."));

        mockMvc.perform(multipart(URL).file(arquivo()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Formato não suportado. Envie PDF, PNG ou JPEG."));
    }

    @Test
    void documentoDeOutraPessoaRetorna404() throws Exception {
        when(contextoService.anexar(eq(ID), any(), any())).thenThrow(new DocumentoNaoEncontradoException());

        mockMvc.perform(multipart(URL).file(arquivo()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarDevolveOsAnexos() throws Exception {
        when(contextoService.listar(eq(ID), any())).thenReturn(List.of(anexo()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("aditivo.pdf"));
    }
}
