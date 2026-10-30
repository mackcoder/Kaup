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

/** Uma mensagem (pergunta do usuário ou resposta da IA) do chat de um documento. */
@Entity
public class MensagemChat {

    // Sequencial de propósito: pergunta e resposta são gravadas no mesmo instante, e o id
    // dá a ordem exata da conversa (o relógio, em milissegundos, não daria).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Apagar o documento apaga a conversa junto (no próprio banco).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Documento documento;

    /** "user" ou "assistant" (ver TurnoDeConversa). */
    private String papel;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    private Instant criadoEm = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    public Long getId() { return id; }
    public Documento getDocumento() { return documento; }
    public void setDocumento(Documento documento) { this.documento = documento; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public Instant getCriadoEm() { return criadoEm; }
}
