package com.kaup.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kaup.dto.RedefinirSenhaRequest;
import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.OperacaoNaoPermitidaException;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.LimiteDeTentativas;

/**
 * Recuperação de senha <b>simulada</b>: ainda não há envio de e-mail, então o "código enviado" é
 * sempre o valor de {@code kaup.recuperacao.codigo} (padrão 676869). Serve só para desenvolvimento:
 * com e-mail real, troque por um código aleatório, com validade, enviado à pessoa.
 *
 * <p>A conta de administrador nunca tem a senha alterada por aqui: para ela a resposta é a mesma
 * de um código errado.
 */
@Service
public class RecuperacaoDeSenhaService {

    static final String CODIGO_INVALIDO = "Código inválido. Confira o e-mail e o código e tente de novo.";

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final FindByIndexNameSessionRepository<?> sessoes;
    private final LimiteDeTentativas limite;
    private final String codigo;

    public RecuperacaoDeSenhaService(UsuarioRepository repository, PasswordEncoder encoder,
                                     FindByIndexNameSessionRepository<?> sessoes, LimiteDeTentativas limite,
                                     @Value("${kaup.recuperacao.codigo:}") String codigo) {
        this.repository = repository;
        this.encoder = encoder;
        this.sessoes = sessoes;
        this.limite = limite;
        this.codigo = codigo == null ? "" : codigo.trim();
    }

    /**
     * Troca a senha se o código confere e encerra as sessões abertas da conta. Qualquer motivo de
     * recusa do código (errado, e-mail sem conta, conta de administrador) responde igual, para
     * não revelar quem tem conta.
     */
    @Transactional
    public void redefinir(RedefinirSenhaRequest pedido) {
        if (codigo.isEmpty()) {
            throw new OperacaoNaoPermitidaException("A recuperação de senha não está disponível.");
        }
        String email = ContaService.emailValido(pedido.email());
        ContaService.senhaValida(pedido.password(), pedido.confirmPassword());

        String chave = "recuperar:" + email;
        limite.verificar(chave);
        Optional<Usuario> conta = repository.findByEmail(email);
        if (!codigoConfere(pedido.code()) || conta.isEmpty() || conta.get().getPapel() == Papel.ADMIN) {
            limite.falhou(chave);
            throw new DadosInvalidosException(CODIGO_INVALIDO);
        }
        limite.acertou(chave);

        Usuario usuario = conta.get();
        usuario.setSenhaHash(encoder.encode(pedido.password()));
        repository.save(usuario);
        sessoes.findByPrincipalName(usuario.getEmail()).keySet().forEach(sessoes::deleteById);
    }

    // Comparação em tempo constante: não deixa o tempo de resposta denunciar quantos dígitos batem.
    private boolean codigoConfere(String informado) {
        String limpo = informado == null ? "" : informado.trim();
        return MessageDigest.isEqual(limpo.getBytes(StandardCharsets.UTF_8), codigo.getBytes(StandardCharsets.UTF_8));
    }
}
