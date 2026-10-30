package com.kaup.dto;

/** Primeiro passo da recuperação: só o e-mail da conta. */
public record RecuperarSenhaRequest(String email) { }
