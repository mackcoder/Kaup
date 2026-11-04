package com.kaup.dto;

/** Segundo passo da recuperação: o código recebido e a nova senha. */
public record RedefinirSenhaRequest(String email, String code, String password, String confirmPassword) { }
