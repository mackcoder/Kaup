package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kaup.dto.CadastroRequest;
import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.EmailJaCadastradoException;
import com.kaup.repository.UsuarioRepository;

class ContaServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private ContaService service;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new ContaService(repository, encoder);
        when(encoder.encode("senha1234")).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CadastroRequest pedido(String nome, String email, String senha, String confirmacao) {
        return new CadastroRequest(nome, email, senha, confirmacao);
    }

    private String mensagemDe(CadastroRequest pedido) {
        return assertThrows(DadosInvalidosException.class, () -> service.cadastrar(pedido)).getMessage();
    }

    @Test
    void cadastraGuardandoSoOHashEOEmailEmMinusculas() {
        Usuario criado = service.cadastrar(pedido("  Ana Souza ", "  ANA@Exemplo.COM ", "senha1234", "senha1234"));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertEquals("ana@exemplo.com", salvo.getValue().getEmail());
        assertEquals("Ana Souza", salvo.getValue().getNome());
        assertEquals("hash-bcrypt", salvo.getValue().getSenhaHash());
        assertEquals(Papel.USER, criado.getPapel()); // quem se cadastra nunca nasce administrador
    }

    @Test
    void senhaCurtaEhRecusada() {
        assertEquals("A senha precisa ter pelo menos 8 caracteres.",
                mensagemDe(pedido("Ana", "ana@exemplo.com", "curta", "curta")));
    }

    @Test
    void senhaLongaDemaisEhRecusada() {
        String longa = "a".repeat(73);
        assertEquals("A senha pode ter até 72 caracteres.",
                mensagemDe(pedido("Ana", "ana@exemplo.com", longa, longa)));
    }

    @Test
    void confirmacaoDiferenteEhRecusada() {
        assertEquals("As senhas não conferem.",
                mensagemDe(pedido("Ana", "ana@exemplo.com", "senha1234", "senha12345")));
        assertEquals("As senhas não conferem.",
                mensagemDe(pedido("Ana", "ana@exemplo.com", "senha1234", null)));
    }

    @Test
    void emailSemDominioOuSemArrobaEhRecusado() {
        for (String ruim : new String[] {"ana", "ana@", "ana@exemplo", "@exemplo.com", "a na@exemplo.com", "", null}) {
            assertEquals("Informe um e-mail válido.",
                    mensagemDe(pedido("Ana", ruim, "senha1234", "senha1234")), "e-mail: " + ruim);
        }
    }

    @Test
    void nomeEmBrancoOuGrandeDemaisEhRecusado() {
        assertEquals("Informe seu nome.", mensagemDe(pedido("   ", "ana@exemplo.com", "senha1234", "senha1234")));
        assertEquals("O nome pode ter até 100 caracteres.",
                mensagemDe(pedido("a".repeat(101), "ana@exemplo.com", "senha1234", "senha1234")));
    }

    @Test
    void emailJaCadastradoNaoCriaNada() {
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class,
                () -> service.cadastrar(pedido("Ana", "ANA@exemplo.com", "senha1234", "senha1234")));

        verify(repository, never()).save(any());
    }
}
