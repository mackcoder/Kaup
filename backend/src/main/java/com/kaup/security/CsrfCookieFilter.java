package com.kaup.security;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Força a criação do cookie XSRF-TOKEN em toda resposta. O Spring Security só o grava quando
 * alguém lê o token; sem isto, a página nunca teria um token para enviar no primeiro POST
 * (o próprio login).
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken(); // ler o token é o que faz o cookie ser enviado
        }
        chain.doFilter(request, response);
    }
}
