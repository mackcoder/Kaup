package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import com.kaup.dto.RedefinirSenhaRequest;
import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.MuitasTentativasException;
import com.kaup.exception.OperacaoNaoPermitidaException;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.LimiteDeTentativas;

@SuppressWarnings({"unchecked", "rawtypes"})
class RecuperacaoDeSenhaServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private FindByIndexNameSessionRepository sessoes;
    private RecuperacaoDeSenhaService service;
    private Usuario ana;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        sessoes = mock(FindByIndexNameSessionRepository.class);
        service = new RecuperacaoDeSenhaService(repository, encoder, sessoes, new LimiteDeTentativas(), "676869");
        when(encoder.encode("novasenha1")).thenReturn("hash-novo");
        when(repository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        ana = new Usuario();
        ana.setEmail("ana@exemplo.com");
        ana.setSenhaHash("hash-antigo");
        when(repository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(ana));

        admin = new Usuario();
        admin.setEmail("admin@exemplo.com");
        admin.setSenhaHash("hash-admin");
        admin.setPapel(Papel.ADMIN);
        when(repository.findByEmail("admin@exemplo.com")).thenReturn(Optional.of(admin));
    }

    private RedefinirSenhaRequest pedido(String email, String codigo, String senha, String confirmacao) {
        return new RedefinirSenhaRequest(email, codigo, senha, confirmacao);
    }

    @Test
    void codigoCertoTrocaASenhaEEncerraAsSessoesDaConta() {
        Session sessao = mock(Session.class);
        when(sessoes.findByPrincipalName("ana@exemplo.com")).thenReturn(Map.of("s1", sessao, "s2", sessao));

        service.redefinir(pedido("Ana@Exemplo.com ", "676869", "novasenha1", "novasenha1"));

        assertEquals("hash-novo", ana.getSenhaHash());
        verify(repository).save(ana);
        verify(sessoes).deleteById("s1");
        verify(sessoes).deleteById("s2");
    }

    @Test
    void codigoErradoNaoTrocaNada() {
        DadosInvalidosException e = assertThrows(DadosInvalidosException.class,
                () -> service.redefinir(pedido("ana@exemplo.com", "111111", "novasenha1", "novasenha1")));

        assertEquals(RecuperacaoDeSenhaService.CODIGO_INVALIDO, e.getMessage());
        assertEquals("hash-antigo", ana.getSenhaHash());
        verify(repository, never()).save(any());
    }

    @Test
    void administradorNuncaTemASenhaAlteradaMesmoComOCodigoCerto() {
        DadosInvalidosException e = assertThrows(DadosInvalidosException.class,
                () -> service.redefinir(pedido("admin@exemplo.com", "676869", "novasenha1", "novasenha1")));

        assertEquals(RecuperacaoDeSenhaService.CODIGO_INVALIDO, e.getMessage()); // igual a um código errado
        assertEquals("hash-admin", admin.getSenhaHash());
        verify(repository, never()).save(any());
        verify(sessoes, never()).deleteById(any());
    }

    @Test
    void emailSemContaRespondeIgualACodigoErrado() {
        when(repository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        DadosInvalidosException e = assertThrows(DadosInvalidosException.class,
                () -> service.redefinir(pedido("ninguem@exemplo.com", "676869", "novasenha1", "novasenha1")));

        assertEquals(RecuperacaoDeSenhaService.CODIGO_INVALIDO, e.getMessage());
    }

    @Test
    void novaSenhaPrecisaSeguirAsMesmasRegrasDoCadastro() {
        assertThrows(DadosInvalidosException.class,
                () -> service.redefinir(pedido("ana@exemplo.com", "676869", "curta", "curta")));
        assertThrows(DadosInvalidosException.class,
                () -> service.redefinir(pedido("ana@exemplo.com", "676869", "novasenha1", "outrasenha1")));
        assertEquals("hash-antigo", ana.getSenhaHash());
    }

    @Test
    void erroRepetidoDeCodigoBloqueiaPorUmInstante() {
        for (int i = 0; i < 5; i++) {
            assertThrows(DadosInvalidosException.class,
                    () -> service.redefinir(pedido("ana@exemplo.com", "000000", "novasenha1", "novasenha1")));
        }

        // bloqueado: nem o código certo vale durante os 10 segundos
        assertThrows(MuitasTentativasException.class,
                () -> service.redefinir(pedido("ana@exemplo.com", "676869", "novasenha1", "novasenha1")));
        assertEquals("hash-antigo", ana.getSenhaHash());
    }

    @Test
    void semCodigoConfiguradoARecuperacaoFicaDesligada() {
        service = new RecuperacaoDeSenhaService(repository, encoder, sessoes, new LimiteDeTentativas(), "");

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> service.redefinir(pedido("ana@exemplo.com", "", "novasenha1", "novasenha1")));
        verify(repository, never()).save(any());
    }
}
