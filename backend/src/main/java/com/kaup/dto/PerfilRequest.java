package com.kaup.dto;

/**
 * Corpo do PATCH do perfil. Todo campo é opcional: só o que vier preenchido é alterado.
 * (O frontend manda o perfil inteiro; um cliente pode mandar só uma parte.)
 */
public record PerfilRequest(
        String name,
        String email,
        String fontSize,
        String explainLevel,
        String income,
        Boolean audio,
        Boolean shareIncomeWithAi) { }
