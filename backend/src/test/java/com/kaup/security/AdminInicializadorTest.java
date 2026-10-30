package com.kaup.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.repository.UsuarioRepository;

class AdminInicializadorTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private AdminInicializador inicializador;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode("ADMIN")).thenReturn("hash-do-admin");
        inicializador = new AdminInicializador(repository, encoder, "ADMIN@ADMIN", "ADMIN", "Administrador");
    }

    @Test
    void criaOAdministradorComEmailEmMinusculasEHash() {
        when(repository.existsByEmail("admin@admin")).thenReturn(false);

        inicializador.run(null);

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertEquals("admin@admin", salvo.getValue().getEmail());
        assertEquals(Papel.ADMIN, salvo.getValue().getPapel());
        assertEquals("hash-do-admin", salvo.getValue().getSenhaHash()); // nunca a senha em claro
    }

    @Test
    void naoMexeNaContaQueJaExiste() {
        when(repository.existsByEmail("admin@admin")).thenReturn(true);

        inicializador.run(null);

        verify(repository, never()).save(any());
    }
}
