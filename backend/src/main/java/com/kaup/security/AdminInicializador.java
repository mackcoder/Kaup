package com.kaup.security;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.repository.UsuarioRepository;
import com.kaup.service.ContaService;

/**
 * Mantém a conta de administrador alinhada com a configuração a cada subida.
 *
 * <ul>
 *   <li>Ligada ({@code kaup.admin.enabled=true}): cria a conta se não existir e, se ela já existe
 *       com outra senha, troca o hash pela de {@code KAUP_ADMIN_PASSWORD}. Assim mudar a variável
 *       vale também num banco antigo.
 *   <li>Desligada: uma conta ADMIN que já exista é desativada (a senha vira um valor aleatório que
 *       ninguém conhece e as sessões abertas dela são apagadas).
 * </ul>
 *
 * O administrador enxerga os documentos de todos.
 */
@Component
public class AdminInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);
    private static final String SENHA_PADRAO = "ADMIN";

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final boolean ligado;
    private final String email;
    private final String senha;
    private final String nome;

    public AdminInicializador(UsuarioRepository repository, PasswordEncoder encoder, JdbcTemplate jdbc,
                              @Value("${kaup.admin.enabled}") boolean ligado,
                              @Value("${kaup.admin.email}") String email,
                              @Value("${kaup.admin.password}") String senha,
                              @Value("${kaup.admin.nome}") String nome) {
        this.repository = repository;
        this.encoder = encoder;
        this.jdbc = jdbc;
        this.ligado = ligado;
        this.email = ContaService.normalizarEmail(email);
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (ligado) {
            garantirAdministrador();
        } else {
            desativarAdministradores();
        }
    }

    private void garantirAdministrador() {
        Usuario existente = repository.findByEmail(email).orElse(null);
        if (existente == null) {
            Usuario admin = new Usuario();
            admin.setNome(nome);
            admin.setEmail(email);
            admin.setSenhaHash(encoder.encode(senha));
            admin.setPapel(Papel.ADMIN);
            repository.save(admin);
            log.info("Conta de administrador criada: {}", email);
        } else if (existente.getPapel() == Papel.ADMIN && !encoder.matches(senha, existente.getSenhaHash())) {
            existente.setSenhaHash(encoder.encode(senha));
            repository.save(existente);
            log.info("Senha da conta de administrador atualizada a partir de KAUP_ADMIN_PASSWORD: {}", email);
        }
        if (SENHA_PADRAO.equals(senha)) {
            log.warn("A conta de administrador usa a senha padrão. Defina KAUP_ADMIN_PASSWORD "
                    + "(ou KAUP_ADMIN_ENABLED=false) antes de publicar o sistema.");
        }
    }

    private void desativarAdministradores() {
        for (Usuario admin : repository.findByPapel(Papel.ADMIN)) {
            admin.setSenhaHash(encoder.encode(UUID.randomUUID().toString()));
            repository.save(admin);
            try {
                jdbc.update("DELETE FROM spring_session WHERE principal_name = ?", admin.getEmail());
            } catch (RuntimeException e) {
                log.warn("Não foi possível apagar as sessões abertas do administrador {}", admin.getEmail(), e);
            }
            log.info("Administrador desativado (KAUP_ADMIN_ENABLED=false): {}", admin.getEmail());
        }
    }
}
