package com.kaup.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
 * Arquivo extra anexado à conversa de um documento (um aditivo, uma tabela, um comprovante).
 * Guarda só o texto extraído, como o documento principal: o arquivo original não é armazenado.
 */
@Entity
public class AnexoContexto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Apagar o documento apaga os anexos junto (no próprio banco).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Documento documento;

    private String nome;

    private String mimeType;

    @Column(columnDefinition = "TEXT")
    private String textoExtraido;

    private Instant criadoEm = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    public Long getId() { return id; }
    public Documento getDocumento() { return documento; }
    public void setDocumento(Documento documento) { this.documento = documento; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getTextoExtraido() { return textoExtraido; }
    public void setTextoExtraido(String textoExtraido) { this.textoExtraido = textoExtraido; }
    public Instant getCriadoEm() { return criadoEm; }
}
