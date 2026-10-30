package com.kaup.dto;

import com.kaup.entity.Usuario;

/**
 * Perfil como o frontend espera (ver profileFromDTO em frontend/src/services/api/dto.ts).
 * O frontend rejeita nulos e valores fora das listas de {@code PerfilOpcoes}.
 *
 * @param shareIncomeWithAi falso quando a pessoa bloqueou o envio da renda à IA
 * @param retentionDays depois de quantos dias os documentos são apagados; 0 se nunca
 */
public record PerfilResponse(
        String name,
        String email,
        String fontSize,
        String explainLevel,
        String income,
        boolean audio,
        boolean shareIncomeWithAi,
        int retentionDays) {

    public static PerfilResponse de(Usuario usuario, int retentionDays) {
        return new PerfilResponse(usuario.getNome(), usuario.getEmail(), usuario.getFontSize(),
                usuario.getExplainLevel(), usuario.getIncome(), usuario.isAudio(), usuario.isCompartilharRenda(),
                retentionDays);
    }
}
