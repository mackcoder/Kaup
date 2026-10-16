package com.kaup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Accounts are loaded from PostgreSQL. Never generate a default development user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class KaupApplication {
    public static void main(String[] args) {
        SpringApplication.run(KaupApplication.class, args);
    }
}
