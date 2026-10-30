package com.kaup.service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kaup.dto.CadastroRequest;
import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.EmailJaCadastradoException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.UsuarioRepository;

/** Cadastro e consulta de contas. */
@Service
public class ContaService {

    // Exige algo@dominio.ext; a conta de administrador é criada por código e não passa por aqui.
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    static final int SENHA_MIN = 8;
    // O BCrypt só considera os 72 primeiros bytes; acima disso senhas diferentes valeriam igual.
    static final int SENHA_MAX = 72;
    static final int NOME_MAX = 100;
    static final int EMAIL_MAX = 254;

    private final UsuarioRepository repository;
    private final DocumentoRepository documentos;
    private final PasswordEncoder encoder;

    public ContaService(UsuarioRepository repository, DocumentoRepository documentos, PasswordEncoder encoder) {
        this.repository = repository;
        this.documentos = documentos;
        this.encoder = encoder;
    }

    @Transactional
    public Usuario cadastrar(CadastroRequest pedido) {
        String nome = nomeValido(pedido.name());
        String email = emailValido(pedido.email());
        senhaValida(pedido.password(), pedido.confirmPassword());
        if (repository.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(encoder.encode(pedido.password()));
        usuario.setPapel(Papel.USER);
        Usuario criado = repository.save(usuario);

        // Documentos de antes das contas não têm dono: o primeiro usuário comum a se cadastrar
        // fica com eles. O administrador não conta, pois já enxerga todos.
        if (repository.countByPapel(Papel.USER) == 1) {
            documentos.atribuirSemDono(criado);
        }
        return criado;
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscar(UUID id) {
        return repository.findById(id);
    }

    /** E-mail sempre em minúsculas e sem espaços nas pontas, como é guardado. */
    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    static String nomeValido(String nome) {
        String limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty()) {
            throw new DadosInvalidosException("Informe seu nome.");
        }
        if (limpo.length() > NOME_MAX) {
            throw new DadosInvalidosException("O nome pode ter até " + NOME_MAX + " caracteres.");
        }
        return limpo;
    }

    static String emailValido(String email) {
        String limpo = normalizarEmail(email);
        if (limpo.length() > EMAIL_MAX || !EMAIL.matcher(limpo).matches()) {
            throw new DadosInvalidosException("Informe um e-mail válido.");
        }
        return limpo;
    }

    static void senhaValida(String senha, String confirmacao) {
        if (senha == null || senha.length() < SENHA_MIN) {
            throw new DadosInvalidosException("A senha precisa ter pelo menos " + SENHA_MIN + " caracteres.");
        }
        if (senha.length() > SENHA_MAX) {
            throw new DadosInvalidosException("A senha pode ter até " + SENHA_MAX + " caracteres.");
        }
        if (!senha.equals(confirmacao)) {
            throw new DadosInvalidosException("As senhas não conferem.");
        }
    }
}
