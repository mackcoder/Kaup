package com.kaup.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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
     * Dono do documento. Fica nulo só nos documentos criados antes das contas, até o primeiro
     * usuário se cadastrar e adotá-los. Apagar a conta apaga os documentos (no próprio banco).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario dono;

    // A mesma coluna, só para leitura: dá o id do dono sem carregar (nem tocar no proxy de) Usuario.
    @Column(name = "dono_id", insertable = false, updatable = false)
    private UUID donoId;

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
    public Usuario getDono() { return dono; }
    public void setDono(Usuario dono) { this.dono = dono; }
    public UUID getDonoId() { return donoId; }
    public String getTextoExtraido() { return textoExtraido; }
    public void setTextoExtraido(String textoExtraido) { this.textoExtraido = textoExtraido; }
    public String getAnaliseJson() { return analiseJson; }
    public void setAnaliseJson(String analiseJson) { this.analiseJson = analiseJson; }
    public Instant getCriadoEm() { return criadoEm; }
}
