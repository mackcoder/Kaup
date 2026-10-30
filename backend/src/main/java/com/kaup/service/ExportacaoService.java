package com.kaup.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaup.dto.ExportacaoResponse;
import com.kaup.dto.ExportacaoResponse.Conta;
import com.kaup.dto.ExportacaoResponse.Mensagem;
import com.kaup.dto.ExportacaoResponse.Preferencias;
import com.kaup.entity.Documento;
import com.kaup.entity.MensagemChat;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.MensagemChatRepository;
import com.kaup.repository.UsuarioRepository;

/** Monta o arquivo "meus dados": a conta, os documentos e as conversas de quem pede, e de mais ninguém. */
@Service
public class ExportacaoService {

    private final UsuarioRepository usuarios;
    private final DocumentoRepository documentos;
    private final MensagemChatRepository mensagens;
    private final ObjectMapper json;

    public ExportacaoService(UsuarioRepository usuarios, DocumentoRepository documentos,
                             MensagemChatRepository mensagens, ObjectMapper json) {
        this.usuarios = usuarios;
        this.documentos = documentos;
        this.mensagens = mensagens;
        this.json = json;
    }

    // Só os documentos da própria pessoa: o administrador, que enxerga os de todos, exporta só os seus.
    @Transactional(readOnly = true)
    public ExportacaoResponse exportar(UUID usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new DadosInvalidosException("A conta não existe mais."));
        List<ExportacaoResponse.Documento> docs = documentos.findByDonoIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(this::documento)
                .toList();
        return new ExportacaoResponse(Instant.now().toString(), conta(usuario), docs);
    }

    private Conta conta(Usuario u) {
        return new Conta(u.getNome(), u.getEmail(), u.getPapel().name(), u.getCriadoEm().toString(),
                new Preferencias(u.getFontSize(), u.getExplainLevel(), u.getIncome(), u.isAudio(),
                        u.isCompartilharRenda()));
    }

    private ExportacaoResponse.Documento documento(Documento d) {
        List<Mensagem> conversa = mensagens.findByDocumentoIdOrderByIdAsc(d.getId()).stream()
                .map(this::mensagem)
                .toList();
        return new ExportacaoResponse.Documento(d.getId().toString(), d.getNome(), d.getCategoryId(), d.getMimeType(),
                d.getProcessingStatus(), d.getCriadoEm().toString(), d.getTextoExtraido(), analise(d), conversa);
    }

    private Mensagem mensagem(MensagemChat m) {
        return new Mensagem(m.getPapel(), m.getConteudo(), m.getCriadoEm().toString());
    }

    private JsonNode analise(Documento d) {
        if (d.getAnaliseJson() == null || d.getAnaliseJson().isBlank()) {
            return null;
        }
        try {
            return json.readTree(d.getAnaliseJson());
        } catch (Exception e) {
            return null; // análise ilegível: o resto do arquivo ainda vale
        }
    }
}
