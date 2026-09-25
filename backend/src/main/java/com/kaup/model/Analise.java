package com.kaup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analises")
public class Analise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(columnDefinition = "TEXT")
    private String textoExtraidoOcr;

    @Column(columnDefinition = "TEXT")
    private String resultadoIa;

    private LocalDateTime dataCriacao;

    public Analise() {
        this.dataCriacao = LocalDateTime.now();
    }

    public Analise(String nomeArquivo, String textoExtraidoOcr, String resultadoIa) {
        this.nomeArquivo = nomeArquivo;
        this.textoExtraidoOcr = textoExtraidoOcr;
        this.resultadoIa = resultadoIa;
        this.dataCriacao = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }

    public String getTextoExtraidoOcr() { return textoExtraidoOcr; }
    public void setTextoExtraidoOcr(String textoExtraidoOcr) { this.textoExtraidoOcr = textoExtraidoOcr; }

    public String getResultadoIa() { return resultadoIa; }
    public void setResultadoIa(String resultadoIa) { this.resultadoIa = resultadoIa; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}