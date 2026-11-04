package com.kaup.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

/** Simula uma pessoa logada (com o tipo de principal que a aplicação usa) num teste de controller. */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithUsuarioLogadoFactory.class)
public @interface WithUsuarioLogado {

    boolean admin() default false;
}
