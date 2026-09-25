package com.kaup.dto;

import java.time.LocalDateTime;

public class AnaliseResponseDTO {

    private Long id;
    private String nomeArquivo;
    private String textoExtraidoOcr;
    private String resultadoIA;
    private LocalDateTime dataCriacao;

    Public AnaliseResponseDTO(
        Long id,
        String nomeArquivo,
        String textoExtraidoOCR,
        String resultadoIA,
        LocalDateTime dataCriacao
    ) {
        this.id = id;
    }

}