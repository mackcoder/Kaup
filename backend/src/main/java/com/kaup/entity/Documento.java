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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Contrato enviado pelo usuário. Guarda só o texto extraído e a análise da IA;
 * o arquivo original não é armazenado.
 */
@Entity
// Reenviar com a mesma Idempotency-Key devolve o documento já criado (nulos não colidem no Postgres).
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"dono_id", "chaveIdempotencia"}))
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

    private String mimeType;

    /** Chave de categoria do frontend (Carros, Casa, Trabalho, Banco, Outros) ou nulo. */
    private String categoryId;

    /** pending, processing, completed ou failed. O envio responde na hora; a leitura roda em segundo plano. */
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

    /** Chave que o frontend gera ao escolher o arquivo, para um reenvio não duplicar o documento. */
    @Column(length = 64)
    private String chaveIdempotencia;

    /** Motivo curto, em português, de o documento ter ficado em "failed". Nulo nos demais. */
    @Column(length = 300)
    private String falha;

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
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public String getFalha() { return falha; }
    public void setFalha(String falha) { this.falha = falha; }
    public String getTextoExtraido() { return textoExtraido; }
    public void setTextoExtraido(String textoExtraido) { this.textoExtraido = textoExtraido; }
    public String getAnaliseJson() { return analiseJson; }
    public void setAnaliseJson(String analiseJson) { this.analiseJson = analiseJson; }
    public Instant getCriadoEm() { return criadoEm; }
}
