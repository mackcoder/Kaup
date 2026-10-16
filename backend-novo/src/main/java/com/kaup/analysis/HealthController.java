package com.kaup.analysis;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping(value = "/api/analise/health", produces = MediaType.TEXT_PLAIN_VALUE)
    public String health() {
        return "Backend do Kaup está rodando.";
    }
}
