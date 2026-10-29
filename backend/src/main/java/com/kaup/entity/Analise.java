package com.kaup.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Analise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeArquivo;

    @Column(columnDefinition = "TEXT")
    private String textoExtraido;

    @Column(columnDefinition = "TEXT")
    private String analiseJson;

    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public String getTextoExtraido() { return textoExtraido; }
    public void setTextoExtraido(String textoExtraido) { this.textoExtraido = textoExtraido; }
    public String getAnaliseJson() {return analiseJson; }
    public void setAnaliseJson(String analiseJson) {this.analiseJson = analiseJson; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

}