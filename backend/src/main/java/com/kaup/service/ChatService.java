package com.kaup.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kaup.dto.MensagemChatResponse;
import com.kaup.entity.Documento;
import com.kaup.entity.MensagemChat;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.MensagemChatRepository;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.UsuarioLogado;

/** Chat sobre um documento: pergunta do usuário, resposta da IA e histórico. */
@Service
public class ChatService {

    static final int TAMANHO_MAX_PERGUNTA = 2000;

    /** Quantas mensagens anteriores a IA recebe junto com a pergunta (5 trocas). */
    static final int HISTORICO_MAX = 10;

    private final DocumentoService documentoService;
    private final MensagemChatRepository repository;
    private final UsuarioRepository usuarios;
    private final ServicoDeIA servicoDeIA;

    public ChatService(DocumentoService documentoService, MensagemChatRepository repository,
                       UsuarioRepository usuarios, ServicoDeIA servicoDeIA) {
        this.documentoService = documentoService;
        this.repository = repository;
        this.usuarios = usuarios;
        this.servicoDeIA = servicoDeIA;
    }

    /**
     * Responde a pergunta com base no contrato e nas últimas mensagens da conversa. Pergunta e
     * resposta só são gravadas juntas, depois que a IA responde: o frontend reenvia o mesmo
     * texto quando a IA falha, e gravar a pergunta antes duplicaria mensagens a cada tentativa.
     * Não é transacional de propósito: a chamada à IA leva segundos e não deve segurar uma
     * conexão do banco.
     */
    public MensagemChatResponse perguntar(String documentoId, String pergunta, UsuarioLogado usuario) {
        String texto = validar(pergunta);
        Documento documento = documentoService.obterDoDono(documentoId, usuario); // 404 se não é seu

        List<MensagemChat> recentes = new ArrayList<>(
                repository.findByDocumentoIdOrderByIdDesc(documento.getId(), PageRequest.of(0, HISTORICO_MAX)));
        Collections.reverse(recentes); // a IA recebe da mais antiga para a mais recente
        List<TurnoDeConversa> historico = recentes.stream()
                .map(m -> new TurnoDeConversa(m.getPapel(), m.getConteudo()))
                .toList();

        // Perfil lido a cada pergunta: mudar o nível de explicação ou bloquear a renda vale já na próxima.
        PreferenciasDeIA preferencias = usuarios.findById(usuario.id())
                .map(PreferenciasDeIA::de).orElse(PreferenciasDeIA.PADRAO);
        String resposta = servicoDeIA.conversar(documento.getTextoExtraido(), historico, texto, preferencias);
        if (resposta == null || resposta.isBlank()) {
            throw new ServicoExternoException("IA", null);
        }

        MensagemChat doUsuario = nova(documento, TurnoDeConversa.USUARIO, texto);
        MensagemChat daIa = nova(documento, TurnoDeConversa.ASSISTENTE, resposta.trim());
        repository.saveAll(List.of(doUsuario, daIa)); // uma transação; o id dá a ordem
        return paraResponse(daIa, documento);
    }

    @Transactional(readOnly = true)
    public List<MensagemChatResponse> historico(String documentoId, UsuarioLogado usuario) {
        Documento documento = documentoService.obterParaLeitura(documentoId, usuario);
        return repository.findByDocumentoIdOrderByIdAsc(documento.getId()).stream()
                .map(m -> paraResponse(m, documento))
                .toList();
    }

    private String validar(String pergunta) {
        String texto = pergunta == null ? "" : pergunta.trim();
        if (texto.isEmpty()) {
            throw new ContratoInvalidoException("Digite uma pergunta.");
        }
        if (texto.length() > TAMANHO_MAX_PERGUNTA) {
            throw new ContratoInvalidoException(
                    "A pergunta pode ter até " + TAMANHO_MAX_PERGUNTA + " caracteres.");
        }
        return texto;
    }

    private MensagemChat nova(Documento documento, String papel, String conteudo) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setDocumento(documento);
        mensagem.setPapel(papel);
        mensagem.setConteudo(conteudo);
        return mensagem;
    }

    // O id do documento vem de quem chama, não da associação preguiçosa da mensagem.
    private MensagemChatResponse paraResponse(MensagemChat mensagem, Documento documento) {
        return new MensagemChatResponse(
                mensagem.getId().toString(),
                documento.getId().toString(),
                mensagem.getPapel(),
                mensagem.getConteudo(),
                mensagem.getCriadoEm());
    }
}
