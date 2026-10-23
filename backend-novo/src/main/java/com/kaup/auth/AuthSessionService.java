package com.kaup.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {
    private final AuthenticationManager manager;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository contexts;
    private final AuthService accounts;

    public AuthSessionService(AuthenticationManager manager, SessionAuthenticationStrategy sessionStrategy,
                              SecurityContextRepository contexts, AuthService accounts) {
        this.manager = manager;
        this.sessionStrategy = sessionStrategy;
        this.contexts = contexts;
        this.accounts = accounts;
    }

    public UserResponse login(AuthRequests.Login input, HttpServletRequest request, HttpServletResponse response) {
        var candidate = UsernamePasswordAuthenticationToken.unauthenticated(input.email(), input.password());
        var authentication = manager.authenticate(candidate);
        UserResponse user = accounts.me(authentication);
        sessionStrategy.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return user;
    }
}
