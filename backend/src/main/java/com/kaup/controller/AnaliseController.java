package com.kaup.controller;

import com.kaup.service.GeminiService;
import com.kaup.service.OcrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analise")
public class AnaliseController {

    private final OcrService ocrService;
    private final GeminiService geminiService;

    public AnaliseController(OcrService ocrService, GeminiService geminiService) {
        this.ocrService = ocrService;
        this.geminiService = geminiService;
    }

    @GetMapping("/health")
    public String checkHealth() {
        return "Backend do Kaup está rodando e integrado!";
    }
}