package com.kaup.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.repository.UsuarioRepository;

class AdminInicializadorTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private JdbcTemplate jdbc;
    private AdminInicializador inicializador;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode("ADMIN")).thenReturn("hash-do-admin");
        jdbc = mock(JdbcTemplate.class);
        inicializador = novo(true, "ADMIN");
    }

    private AdminInicializador novo(boolean ligado, String senha) {
        return new AdminInicializador(repository, encoder, jdbc, ligado, "ADMIN@ADMIN", senha, "Administrador");
    }

    private Usuario admin(String hash) {
        Usuario admin = new Usuario();
        admin.setEmail("admin@admin");
        admin.setPapel(Papel.ADMIN);
        admin.setSenhaHash(hash);
        return admin;
    }

    @Test
    void criaOAdministradorComEmailEmMinusculasEHash() {
        when(repository.findByEmail("admin@admin")).thenReturn(Optional.empty());

        inicializador.run(null);

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertEquals("admin@admin", salvo.getValue().getEmail());
        assertEquals(Papel.ADMIN, salvo.getValue().getPapel());
        assertEquals("hash-do-admin", salvo.getValue().getSenhaHash()); // nunca a senha em claro
    }

    @Test
    void naoMexeNaContaQueJaExisteComAMesmaSenha() {
        Usuario existente = admin("hash-do-admin");
        when(repository.findByEmail("admin@admin")).thenReturn(Optional.of(existente));
        when(encoder.matches("ADMIN", "hash-do-admin")).thenReturn(true);

        inicializador.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void atualizaOHashQuandoASenhaDaVariavelMudou() {
        Usuario existente = admin("hash-antigo");
        when(repository.findByEmail("admin@admin")).thenReturn(Optional.of(existente));
        when(encoder.matches("ADMIN", "hash-antigo")).thenReturn(false);

        inicializador.run(null);

        verify(repository).save(existente);
        assertEquals("hash-do-admin", existente.getSenhaHash());
    }

    @Test
    void naoTrocaASenhaDeUmaContaComumComOEmailDoAdministrador() {
        Usuario comum = admin("hash-comum");
        comum.setPapel(Papel.USER);
        when(repository.findByEmail("admin@admin")).thenReturn(Optional.of(comum));

        inicializador.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void desligadoDesativaOAdministradorQueJaExisteEApagaSuasSessoes() {
        Usuario existente = admin("hash-do-admin");
        when(repository.findByPapel(Papel.ADMIN)).thenReturn(List.of(existente));
        when(encoder.encode(anyString())).thenReturn("hash-inutilizavel");

        novo(false, "ADMIN").run(null);

        verify(repository).save(existente);
        assertEquals("hash-inutilizavel", existente.getSenhaHash());
        verify(jdbc).update(anyString(), eq("admin@admin"));
        verify(repository, never()).findByEmail(any());
    }
}
