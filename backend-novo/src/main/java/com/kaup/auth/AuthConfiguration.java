package com.kaup.auth;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
public class AuthConfiguration {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    PasswordEncoder passwordEncoder(@Value("${auth.bcrypt-strength:12}") int strength) {
        if (strength < 10 || strength > 14) {
            throw new IllegalArgumentException("BCrypt strength must be between 10 and 14");
        }
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthService service, PasswordEncoder passwords) {
        var provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(passwords);
        return new ProviderManager(provider);
    }

    @Bean
    CookieSerializer cookieSerializer(ServerProperties server) {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName("KAUPSESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(Boolean.TRUE.equals(server.getServlet().getSession().getCookie().getSecure()));
        serializer.setUseBase64Encoding(false);
        return serializer;
    }
}
