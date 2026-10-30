package com.kaup.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kaup.dto.PerfilRequest;
import com.kaup.dto.PerfilResponse;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.EmailJaCadastradoException;
import com.kaup.repository.UsuarioRepository;

/** Consulta e atualiza o perfil (nome, e-mail e preferências) de quem está logado. */
@Service
public class PerfilService {

    private final UsuarioRepository repository;

    public PerfilService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obter(UUID usuarioId) {
        return PerfilResponse.de(carregar(usuarioId));
    }

    /** Aplica só os campos preenchidos; qualquer valor inválido recusa o pedido inteiro. */
    @Transactional
    public PerfilResponse atualizar(UUID usuarioId, PerfilRequest pedido) {
        Usuario usuario = carregar(usuarioId);

        // Valida tudo antes de alterar qualquer coisa.
        String nome = pedido.name() != null ? ContaService.nomeValido(pedido.name()) : null;
        String email = pedido.email() != null ? ContaService.emailValido(pedido.email()) : null;
        String fontSize = opcao(pedido.fontSize(), PerfilOpcoes.TAMANHOS_DE_LETRA, "tamanho da letra");
        String explainLevel = opcao(pedido.explainLevel(), PerfilOpcoes.NIVEIS_DE_EXPLICACAO, "nível de explicação");
        String income = opcao(pedido.income(), PerfilOpcoes.FAIXAS_DE_RENDA, "faixa de renda");
        if (email != null && !email.equals(usuario.getEmail())
                && repository.findByEmail(email).filter(outro -> !outro.getId().equals(usuarioId)).isPresent()) {
            throw new EmailJaCadastradoException();
        }

        if (nome != null) usuario.setNome(nome);
        if (email != null) usuario.setEmail(email);
        if (fontSize != null) usuario.setFontSize(fontSize);
        if (explainLevel != null) usuario.setExplainLevel(explainLevel);
        if (income != null) usuario.setIncome(income);
        if (pedido.audio() != null) usuario.setAudio(pedido.audio());
        if (pedido.shareIncomeWithAi() != null) usuario.setCompartilharRenda(pedido.shareIncomeWithAi());
        return PerfilResponse.de(repository.save(usuario));
    }

    private Usuario carregar(UUID usuarioId) {
        return repository.findById(usuarioId)
                .orElseThrow(() -> new DadosInvalidosException("A conta não existe mais."));
    }

    private String opcao(String valor, Set<String> aceitos, String campo) {
        if (valor == null) {
            return null;
        }
        if (!aceitos.contains(valor)) {
            throw new DadosInvalidosException("Valor inválido para " + campo + ".");
        }
        return valor;
    }
}
