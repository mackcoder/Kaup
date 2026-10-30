package com.kaup.security;

import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.kaup.entity.Papel;

public class WithUsuarioLogadoFactory implements WithSecurityContextFactory<WithUsuarioLogado> {

    /** Id da pessoa simulada, para os testes conferirem quem chegou ao serviço. */
    public static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Override
    public SecurityContext createSecurityContext(WithUsuarioLogado anotacao) {
        UsuarioLogado usuario = new UsuarioLogado(ID, "ana@exemplo.com",
                anotacao.admin() ? Papel.ADMIN : Papel.USER, null);
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                usuario, null, usuario.getAuthorities()));
        return contexto;
    }
}
