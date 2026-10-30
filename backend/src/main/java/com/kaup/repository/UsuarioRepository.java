package com.kaup.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** O e-mail é guardado em minúsculas; quem chama normaliza antes. */
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByPapel(Papel papel);
}
