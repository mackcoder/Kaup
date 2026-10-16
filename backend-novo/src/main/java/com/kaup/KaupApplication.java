package com.kaup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// No generated password or provisional login. Real accounts arrive in phase 2.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class KaupApplication {
    public static void main(String[] args) {
        SpringApplication.run(KaupApplication.class, args);
    }
}
