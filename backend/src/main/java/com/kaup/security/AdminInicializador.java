package com.kaup.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.repository.UsuarioRepository;
import com.kaup.service.ContaService;

/**
 * Cria a conta de administrador na subida, se ainda não existir. Só cria: nunca altera uma
 * conta existente. O administrador enxerga os documentos de todos.
 */
@Component
@ConditionalOnProperty(name = "kaup.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);
    private static final String SENHA_PADRAO = "ADMIN";

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final String email;
    private final String senha;
    private final String nome;

    public AdminInicializador(UsuarioRepository repository, PasswordEncoder encoder,
                              @Value("${kaup.admin.email}") String email,
                              @Value("${kaup.admin.password}") String senha,
                              @Value("${kaup.admin.nome}") String nome) {
        this.repository = repository;
        this.encoder = encoder;
        this.email = ContaService.normalizarEmail(email);
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!repository.existsByEmail(email)) {
            Usuario admin = new Usuario();
            admin.setNome(nome);
            admin.setEmail(email);
            admin.setSenhaHash(encoder.encode(senha));
            admin.setPapel(Papel.ADMIN);
            repository.save(admin);
            log.info("Conta de administrador criada: {}", email);
        }
        if (SENHA_PADRAO.equals(senha)) {
            log.warn("A conta de administrador usa a senha padrão. Defina KAUP_ADMIN_PASSWORD "
                    + "(ou KAUP_ADMIN_ENABLED=false) antes de publicar o sistema.");
        }
    }
}
