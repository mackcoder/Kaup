package com.kaup.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kaup.dto.MensagemChatResponse;
import com.kaup.dto.PerguntaRequest;
import com.kaup.service.ChatService;

/** Chat sobre um documento (ver frontend/src/services/api/endpoints.ts). */
@RestController
@RequestMapping("/api/documentos/{id}/chat/mensagens")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Conversa completa, da mensagem mais antiga para a mais recente. */
    @GetMapping
    public List<MensagemChatResponse> historico(@PathVariable String id) {
        return chatService.historico(id);
    }

    /** Recebe a pergunta e devolve só a resposta da IA; a pergunta fica no histórico. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemChatResponse perguntar(@PathVariable String id, @RequestBody PerguntaRequest pergunta) {
        return chatService.perguntar(id, pergunta.message());
    }
}
