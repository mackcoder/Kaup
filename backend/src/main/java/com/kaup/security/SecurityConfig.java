package com.kaup.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * API protegida por sessão em cookie. O frontend lê o cookie XSRF-TOKEN e o devolve no
 * cabeçalho X-XSRF-TOKEN em toda requisição que altera dados (inclusive o login).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider();
        provedor.setUserDetailsService(usuarios);
        provedor.setPasswordEncoder(encoder);
        return new ProviderManager(provedor);
    }

    @Bean
    public LimitePorIp limitePorIp(@Value("${kaup.limite.ip-por-minuto}") int porMinuto,
                                   @Value("${kaup.limite.auth-por-minuto}") int authPorMinuto,
                                   @Value("${kaup.limite.cadastros-por-hora}") int cadastrosPorHora) {
        return new LimitePorIp(porMinuto, authPorMinuto, cadastrosPorHora);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contextos,
                                           ObjectMapper json, LimitePorIp limitePorIp,
                                           @Value("${kaup.limite.saltos-de-proxy}") int saltosDeProxy)
            throws Exception {
        http
            .addFilterBefore(new LimitePorIpFilter(limitePorIp, json, saltosDeProxy), DisableEncodeUrlFilter.class)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .securityContext(contexto -> contexto.securityContextRepository(contextos))
            .authorizeHttpRequests(regras -> regras
                .requestMatchers("/api/analise/health", "/api/auth/cadastro", "/api/auth/entrar",
                        "/api/auth/recuperar/solicitar", "/api/auth/recuperar/redefinir", "/error")
                    .permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint((request, response, e) -> responder(response, json,
                        HttpStatus.UNAUTHORIZED, "Entre na sua conta para continuar."))
                .accessDeniedHandler((request, response, e) -> responder(response, json,
                        HttpStatus.FORBIDDEN, "Requisição recusada. Recarregue a página e tente de novo.")))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    // Mesmo formato (RFC 7807) dos demais erros da API: o frontend lê o campo "detail".
    private void responder(HttpServletResponse response, ObjectMapper json, HttpStatus status, String detalhe)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detalhe));
    }
}
