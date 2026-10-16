package com.kaup.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

import com.kaup.common.error.ApiError;
import com.kaup.common.web.RequestIdFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    WebSecurityCustomizer rejectedRequestHandling(ObjectMapper mapper) {
        // sendError would dispatch the rejected method back through the same firewall.
        return web -> web.requestRejectedHandler((request, response, exception) ->
                writeError(mapper, request, response, HttpStatus.BAD_REQUEST));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/analise/health").permitAll()
                        .anyRequest().authenticated())
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(mapper, request, response, HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(mapper, request, response, HttpStatus.FORBIDDEN)))
                .build();
        // CSRF remains enabled; no default user, login flow or dev bypass.
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest request,
                                   HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setHeader(RequestIdFilter.HEADER, RequestIdFilter.requestId(request));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiError.forStatus(status, RequestIdFilter.requestId(request)));
    }
}
