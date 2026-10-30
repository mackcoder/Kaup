package com.kaup.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.kaup.dto.AnaliseResponse;
import com.kaup.dto.AnaliseResponse.PontoChave;
import com.kaup.dto.DocumentoResponse;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.service.DocumentoService;

@WebMvcTest(DocumentoController.class)
@WithMockUser
class DocumentoControllerTest {

    private static final String ID = "3f2a9c1e-5b7d-4c8a-9e0f-1a2b3c4d5e6f";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentoService documentoService;

    private DocumentoResponse documento() {
        return new DocumentoResponse(ID, "contrato.pdf", null, "completed",
                Instant.parse("2026-10-30T12:00:00Z"), "Contrato de aluguel.", "warning");
    }

    private MockMultipartFile arquivo() {
        return new MockMultipartFile("file", "contrato.pdf", "application/pdf", "conteudo".getBytes());
    }

    @Test
    void uploadRetorna201NoFormatoQueOFrontendValida() throws Exception {
        when(documentoService.criar(any(), any())).thenReturn(documento());

        mockMvc.perform(multipart("/api/documentos").file(arquivo()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/documentos/" + ID))
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$", hasKey("categoryId"))) // o frontend exige a chave, mesmo nula
                .andExpect(jsonPath("$.categoryId").value(nullValue()))
                .andExpect(jsonPath("$.processingStatus").value("completed"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-30T12:00:00Z"));
    }

    @Test
    void uploadRepassaACategoriaInformada() throws Exception {
        when(documentoService.criar(any(), eq("Carros"))).thenReturn(documento());

        mockMvc.perform(multipart("/api/documentos").file(arquivo()).with(csrf()).param("categoryId", "Carros"))
                .andExpect(status().isCreated());

        verify(documentoService).criar(any(), eq("Carros"));
    }

    @Test
    void contratoInvalidoRetorna400ComMensagemEmPortugues() throws Exception {
        when(documentoService.criar(any(), any()))
                .thenThrow(new ContratoInvalidoException("Formato não suportado. Envie PDF, PNG ou JPEG."));

        mockMvc.perform(multipart("/api/documentos").file(arquivo()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Formato não suportado. Envie PDF, PNG ou JPEG."));
    }

    @Test
    void listaRetornaArray() throws Exception {
        when(documentoService.listar()).thenReturn(List.of(documento()));

        mockMvc.perform(get("/api/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ID));
    }

    @Test
    void buscaPorIdInexistenteRetorna404() throws Exception {
        when(documentoService.buscar("nao-existe")).thenThrow(new DocumentoNaoEncontradoException());

        mockMvc.perform(get("/api/documentos/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Documento não encontrado."));
    }

    @Test
    void analiseRetornaResumoEPontos() throws Exception {
        when(documentoService.analise(ID)).thenReturn(new AnaliseResponse(ID, "completed",
                "Resumo.", List.of(new PontoChave("kp-1", "Valor", "R$ 10,00", "info")), null));

        mockMvc.perform(get("/api/documentos/" + ID + "/analise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(ID))
                .andExpect(jsonPath("$.summary").value("Resumo."))
                .andExpect(jsonPath("$.keyPoints[0].severity").value("info"))
                .andExpect(jsonPath("$", hasKey("ocrQualityWarning")));
    }
}
