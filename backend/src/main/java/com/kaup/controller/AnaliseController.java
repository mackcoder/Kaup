package com.kaup.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analise")
public class AnaliseController {

    // Endpoint de teste para validar se o servidor está no ar
    @GetMapping("/health")
    public String checkHealth() {
        return "Backend do Kaup está rodando!";
    }
}
