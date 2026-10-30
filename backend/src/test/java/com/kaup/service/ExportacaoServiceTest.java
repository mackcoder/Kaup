package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaup.dto.ExportacaoResponse;
import com.kaup.entity.Documento;
import com.kaup.entity.MensagemChat;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.repository.AnexoContextoRepository;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.MensagemChatRepository;
import com.kaup.repository.UsuarioRepository;

class ExportacaoServiceTest {

    private final UUID anaId = UUID.randomUUID();
    private final UUID docId = UUID.randomUUID();
    private UsuarioRepository usuarios;
    private DocumentoRepository documentos;
    private MensagemChatRepository mensagens;
    private ExportacaoService service;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        documentos = mock(DocumentoRepository.class);
        mensagens = mock(MensagemChatRepository.class);
        service = new ExportacaoService(usuarios, documentos, mensagens, mock(AnexoContextoRepository.class), new ObjectMapper());

        ana = new Usuario();
        ReflectionTestUtils.setField(ana, "id", anaId);
        ana.setNome("Ana Souza");
        ana.setEmail("ana@exemplo.com");
        ana.setSenhaHash("$2a$10$hash-secreto");
        when(usuarios.findById(anaId)).thenReturn(Optional.of(ana));
    }

    private Documento documento(String analise) {
        Documento d = new Documento();
        ReflectionTestUtils.setField(d, "id", docId);
        d.setNome("contrato.pdf");
        d.setMimeType("application/pdf");
        d.setCategoryId("Carros");
        d.setProcessingStatus("completed");
        d.setTextoExtraido("CLÁUSULA 1");
        d.setAnaliseJson(analise);
        return d;
    }

    @Test
    void exportaContaPreferenciasDocumentosAnaliseEConversa() {
        when(documentos.findByDonoIdOrderByCriadoEmDesc(anaId)).thenReturn(List.of(documento("{\"resumo\":\"Um resumo\"}")));
        MensagemChat pergunta = new MensagemChat();
        pergunta.setPapel("user");
        pergunta.setConteudo("Qual a multa?");
        MensagemChat resposta = new MensagemChat();
        resposta.setPapel("assistant");
        resposta.setConteudo("10%");
        when(mensagens.findByDocumentoIdOrderByIdAsc(docId)).thenReturn(List.of(pergunta, resposta));

        ExportacaoResponse r = service.exportar(anaId);

        assertEquals("ana@exemplo.com", r.account().email());
        assertEquals("USER", r.account().role());
        assertEquals("large", r.account().preferences().fontSize());
        assertEquals(1, r.documents().size());
        ExportacaoResponse.Documento d = r.documents().get(0);
        assertEquals("contrato.pdf", d.name());
        assertEquals("CLÁUSULA 1", d.extractedText());
        assertEquals("Um resumo", d.analysis().get("resumo").asText());
        assertEquals(List.of("user", "assistant"), d.messages().stream().map(ExportacaoResponse.Mensagem::role).toList());
    }

    @Test
    void naoVazaASenhaNemOHash() throws Exception {
        when(documentos.findByDonoIdOrderByCriadoEmDesc(anaId)).thenReturn(List.of());

        String json = new ObjectMapper().writeValueAsString(service.exportar(anaId));

        assertFalse(json.contains("hash-secreto"));
        assertFalse(json.toLowerCase().contains("senha"));
        assertFalse(json.toLowerCase().contains("password"));
    }

    @Test
    void buscaSoOsDocumentosDoProprioUsuario() {
        when(documentos.findByDonoIdOrderByCriadoEmDesc(anaId)).thenReturn(List.of());

        service.exportar(anaId);

        verify(documentos).findByDonoIdOrderByCriadoEmDesc(anaId);
        verify(documentos, never()).findAll();
        verify(documentos, never()).findAllByOrderByCriadoEmDesc();
    }

    @Test
    void analiseIlegivelNaoDerrubaOArquivo() {
        when(documentos.findByDonoIdOrderByCriadoEmDesc(anaId)).thenReturn(List.of(documento("{quebrado")));

        ExportacaoResponse r = service.exportar(anaId);

        assertNull(r.documents().get(0).analysis());
        assertTrue(r.documents().get(0).messages().isEmpty());
    }

    @Test
    void contaInexistenteDaErro() {
        assertThrows(DadosInvalidosException.class, () -> service.exportar(UUID.randomUUID()));
    }
}
