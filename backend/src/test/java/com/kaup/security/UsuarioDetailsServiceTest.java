package com.kaup.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.repository.UsuarioRepository;

class UsuarioDetailsServiceTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final UsuarioDetailsService service = new UsuarioDetailsService(repository);

    @Test
    void procuraOEmailEmMinusculasESemEspacos() {
        Usuario admin = new Usuario();
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        admin.setEmail("admin@admin");
        admin.setSenhaHash("hash");
        admin.setPapel(Papel.ADMIN);
        when(repository.findByEmail("admin@admin")).thenReturn(Optional.of(admin));

        UserDetails detalhes = service.loadUserByUsername("  ADMIN@ADMIN ");

        assertEquals("admin@admin", detalhes.getUsername());
        assertEquals("hash", detalhes.getPassword());
        assertTrue(detalhes.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void emailInexistenteLancaUsernameNotFound() {
        when(repository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ninguem@exemplo.com"));
    }

    @Test
    void ohashDaSenhaSomeDepoisDaAutenticacao() {
        UsuarioLogado logado = new UsuarioLogado(UUID.randomUUID(), "ana@exemplo.com", Papel.USER, "hash");

        logado.eraseCredentials();

        assertNull(logado.getPassword()); // é o que fica guardado na sessão
    }
}
