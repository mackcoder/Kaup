package com.kaup.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Contrato enviado pelo usuário. Guarda só o texto extraído e a análise da IA;
 * o arquivo original não é armazenado.
 */
@Entity
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

    private String mimeType;

    /** Chave de categoria do frontend (Carros, Casa, Trabalho, Banco, Outros) ou nulo. */
    private String categoryId;

    /** pending, processing, completed ou failed (hoje o fluxo é síncrono: sempre completed). */
    private String processingStatus;

    /**
     * Dono do documento. Fica nulo até existir autenticação; já existe para que a conta
     * entre sem mudar a tabela. Nada o preenche nem filtra por ele ainda.
     */
    private String usuarioId;

    @Column(columnDefinition = "TEXT")
    private String textoExtraido;

    @Column(columnDefinition = "TEXT")
    private String analiseJson;

    // Milissegundos bastam e evitam frações de segundo que o Date.parse do navegador não espera.
    private Instant criadoEm = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getTextoExtraido() { return textoExtraido; }
    public void setTextoExtraido(String textoExtraido) { this.textoExtraido = textoExtraido; }
    public String getAnaliseJson() { return analiseJson; }
    public void setAnaliseJson(String analiseJson) { this.analiseJson = analiseJson; }
    public Instant getCriadoEm() { return criadoEm; }
}
