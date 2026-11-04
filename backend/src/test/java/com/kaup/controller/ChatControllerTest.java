package com.kaup.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.kaup.dto.MensagemChatResponse;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.security.WithUsuarioLogado;
import com.kaup.service.ChatService;

@WebMvcTest(ChatController.class)
@WithUsuarioLogado
class ChatControllerTest {

    private static final String ID = "3f2a9c1e-5b7d-4c8a-9e0f-1a2b3c4d5e6f";
    private static final String URL = "/api/documentos/" + ID + "/chat/mensagens";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    private MensagemChatResponse mensagem(String id, String role, String conteudo) {
        return new MensagemChatResponse(id, ID, role, conteudo, Instant.parse("2026-10-30T12:00:00Z"));
    }

    @Test
    void perguntaRetorna201ComARespostaNoFormatoQueOFrontendValida() throws Exception {
        when(chatService.perguntar(eq(ID), eq("Qual é a multa?"), any())).thenReturn(mensagem("7", "assistant", "10%."));

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Qual é a multa?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("7"))
                .andExpect(jsonPath("$.documentId").value(ID))
                .andExpect(jsonPath("$.role").value("assistant"))
                .andExpect(jsonPath("$.content").value("10%."))
                .andExpect(jsonPath("$.createdAt").value("2026-10-30T12:00:00Z"));

        verify(chatService).perguntar(eq(ID), eq("Qual é a multa?"), any());
    }

    @Test
    void perguntaInvalidaRetorna400ComMensagemEmPortugues() throws Exception {
        when(chatService.perguntar(eq(ID), eq(" "), any())).thenThrow(new ContratoInvalidoException("Digite uma pergunta."));

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Digite uma pergunta."));
    }

    @Test
    void corpoSemJsonValidoRetorna400() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("isso não é json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void falhaDaIaRetorna502() throws Exception {
        when(chatService.perguntar(eq(ID), eq("oi"), any())).thenThrow(new ServicoExternoException("IA (Gemini)", null));

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"oi\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void documentoInexistenteRetorna404() throws Exception {
        when(chatService.perguntar(eq(ID), eq("oi"), any())).thenThrow(new DocumentoNaoEncontradoException());
        when(chatService.historico(eq(ID), any())).thenThrow(new DocumentoNaoEncontradoException());

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"oi\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL)).andExpect(status().isNotFound());
    }

    @Test
    void historicoDevolveAsDuasPontasDaConversa() throws Exception {
        when(chatService.historico(eq(ID), any())).thenReturn(List.of(
                mensagem("1", "user", "Qual é a multa?"),
                mensagem("2", "assistant", "10%.")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[1].role").value("assistant"));
    }
}
