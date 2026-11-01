package com.kaup.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaup.service.AnaliseService;

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
}
