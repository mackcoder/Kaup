package com.kaup.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnexoResponse;
import com.kaup.security.UsuarioLogado;
import com.kaup.service.ContextoService;

/** Anexos de contexto da conversa de um documento (ver frontend/src/services/api/endpoints.ts). */
@RestController
@RequestMapping("/api/documentos/{id}/contexto")
public class ContextoController {

    private final ContextoService contextoService;

    public ContextoController(ContextoService contextoService) {
        this.contextoService = contextoService;
    }

    /** Anexos do documento, do mais antigo para o mais recente (o contrato principal não entra). */
    @GetMapping
    public List<AnexoResponse> listar(@PathVariable String id, @AuthenticationPrincipal UsuarioLogado usuario) {
        return contextoService.listar(id, usuario);
    }

    /** Recebe o arquivo (PDF, PNG ou JPEG), lê o texto por OCR e o junta à conversa. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoResponse anexar(@PathVariable String id, @RequestParam("file") MultipartFile arquivo,
                                @AuthenticationPrincipal UsuarioLogado usuario) throws IOException {
        return contextoService.anexar(id, arquivo, usuario);
    }
}
