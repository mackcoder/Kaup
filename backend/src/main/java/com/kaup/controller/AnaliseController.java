package com.kaup.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnaliseContratoResponse;
import com.kaup.dto.TextoContratoRequest;
import com.kaup.service.AnaliseService;
import com.kaup.service.PreferenciasDeIA;

@RestController
@RequestMapping("/api/analise")
public class AnaliseController {

    private final AnaliseService analiseService;

    public AnaliseController(AnaliseService analiseService) {
        this.analiseService = analiseService;
    }

    @GetMapping("/health")
    public String checkHealth() {
        return "Backend do Kaup está rodando e integrado!";
    }

    /** Fluxo completo: arquivo do contrato -> OCR -> IA -> análise estruturada. */
    @PostMapping(value = "/contrato", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnaliseContratoResponse analisarContrato(@RequestParam("arquivo") MultipartFile arquivo)
            throws IOException {
        return analiseService.analisarArquivo(arquivo, PreferenciasDeIA.PADRAO);
    }

    /** Mesmo fluxo a partir de texto já extraído (pula o OCR). Útil para testar a IA. */
    @PostMapping(value = "/texto", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnaliseContratoResponse analisarTexto(@RequestBody TextoContratoRequest request) {
        return analiseService.analisarTexto(request.texto(), PreferenciasDeIA.PADRAO);
    }
}
