package com.kaup.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.kaup.entity.Papel;

/**
 * Quem está logado, como fica guardado na sessão. Leva só o necessário (id, e-mail, papel);
 * o hash da senha existe apenas durante a autenticação e é apagado logo depois.
 */
public class UsuarioLogado implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final Papel papel;
    private String senhaHash;

    public UsuarioLogado(UUID id, String email, Papel papel, String senhaHash) {
        this.id = id;
        this.email = email;
        this.papel = papel;
        this.senhaHash = senhaHash;
    }

    public UUID id() { return id; }
    public Papel papel() { return papel; }
    public boolean isAdmin() { return papel == Papel.ADMIN; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() { return senhaHash; }

    @Override
    public String getUsername() { return email; }

    // Não há bloqueio, expiração nem desativação de contas.
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public void eraseCredentials() { this.senhaHash = null; }
}
