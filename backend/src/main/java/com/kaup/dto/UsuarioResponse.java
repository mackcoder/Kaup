package com.kaup.dto;

import com.kaup.entity.Usuario;

/**
 * Quem está logado, como o frontend espera.
 *
 * @param role "USER" ou "ADMIN"
 */
public record UsuarioResponse(String id, String name, String email, String role) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId().toString(), usuario.getNome(),
                usuario.getEmail(), usuario.getPapel().name());
    }
}
