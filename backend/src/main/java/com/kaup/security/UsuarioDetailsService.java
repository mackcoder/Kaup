package com.kaup.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.kaup.repository.UsuarioRepository;

/** Carrega a conta pelo e-mail para o Spring Security conferir a senha. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email.trim().toLowerCase())
                .map(u -> new UsuarioLogado(u.getId(), u.getEmail(), u.getPapel(), u.getSenhaHash()))
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada."));
    }
}
