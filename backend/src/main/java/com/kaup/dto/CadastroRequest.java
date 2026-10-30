package com.kaup.dto;

/** Corpo do cadastro. A confirmação de senha também é conferida no servidor. */
public record CadastroRequest(String name, String email, String password, String confirmPassword) { }
