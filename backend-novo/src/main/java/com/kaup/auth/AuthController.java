package com.kaup.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {
    private final AuthService accounts;
    private final AuthSessionService sessions;
    private final AuthRateLimiter limiter;

    public AuthController(AuthService accounts, AuthSessionService sessions, AuthRateLimiter limiter) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.limiter = limiter;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getHeaderName(), token.getToken()); }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody AuthRequests.Register input, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr());
        return accounts.register(input);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse login(@Valid @RequestBody AuthRequests.Login input, HttpServletRequest request,
                              HttpServletResponse response) {
        limiter.check(request.getRemoteAddr());
        return sessions.login(input, request, response);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) { return accounts.me(authentication); }

    public record CsrfResponse(String headerName, String token) { }
}
