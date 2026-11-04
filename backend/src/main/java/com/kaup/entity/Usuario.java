package com.kaup.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Conta de usuário, com as preferências do perfil. */
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Identificador de login; sempre em minúsculas. */
    @Column(unique = true, nullable = false)
    private String email;

    private String nome;

    /** Hash BCrypt; a senha em si nunca é guardada. */
    @Column(nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel = Papel.USER;

    // Preferências (valores aceitos em PerfilOpcoes).
    private String fontSize = "large";
    private String explainLevel = "simple";
    private String income = "prefer_not";
    private boolean audio = false;

    /** Quando falso, a renda nunca é enviada à IA, mesmo que informada. */
    private boolean compartilharRenda = true;

    private Instant criadoEm = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Papel getPapel() { return papel; }
    public void setPapel(Papel papel) { this.papel = papel; }
    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; }
    public String getExplainLevel() { return explainLevel; }
    public void setExplainLevel(String explainLevel) { this.explainLevel = explainLevel; }
    public String getIncome() { return income; }
    public void setIncome(String income) { this.income = income; }
    public boolean isAudio() { return audio; }
    public void setAudio(boolean audio) { this.audio = audio; }
    public boolean isCompartilharRenda() { return compartilharRenda; }
    public void setCompartilharRenda(boolean compartilharRenda) { this.compartilharRenda = compartilharRenda; }
    public Instant getCriadoEm() { return criadoEm; }
}
