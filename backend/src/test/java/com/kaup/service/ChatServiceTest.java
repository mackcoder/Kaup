package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.kaup.dto.MensagemChatResponse;
import com.kaup.entity.Documento;
import com.kaup.entity.Papel;
import com.kaup.entity.MensagemChat;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.MensagemChatRepository;
import com.kaup.security.UsuarioLogado;

class ChatServiceTest {

    private final UUID documentoId = UUID.randomUUID();
    private DocumentoService documentoService;
    private MensagemChatRepository repository;
    private ServicoDeIA servicoDeIA;
    private ChatService service;
    private Documento documento;
    private final UsuarioLogado usuario = new UsuarioLogado(UUID.randomUUID(), "ana@exemplo.com", Papel.USER, null);

    @BeforeEach
    void preparar() {
        documentoService = mock(DocumentoService.class);
        repository = mock(MensagemChatRepository.class);
        servicoDeIA = mock(ServicoDeIA.class);
        service = new ChatService(documentoService, repository, servicoDeIA);

        documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", documentoId);
        documento.setTextoExtraido("texto do contrato");
        when(documentoService.obterDoDono(eq(documentoId.toString()), any())).thenReturn(documento);
        when(documentoService.obterParaLeitura(eq(documentoId.toString()), any())).thenReturn(documento);

        when(repository.findByDocumentoIdOrderByIdDesc(eq(documentoId), any(Pageable.class))).thenReturn(List.of());
        when(servicoDeIA.conversar(anyString(), anyList(), anyString())).thenReturn("  A multa é de 10%.  ");
        // Simula o banco: saveAll atribui ids crescentes, na ordem da lista.
        when(repository.saveAll(anyList())).thenAnswer(invocacao -> {
            List<MensagemChat> mensagens = new ArrayList<>(invocacao.getArgument(0));
            long proximo = 1;
            for (MensagemChat m : mensagens) {
                ReflectionTestUtils.setField(m, "id", proximo++);
            }
            return mensagens;
        });
    }

    private MensagemChat mensagem(long id, String papel, String conteudo) {
        MensagemChat m = new MensagemChat();
        ReflectionTestUtils.setField(m, "id", id);
        m.setDocumento(documento);
        m.setPapel(papel);
        m.setConteudo(conteudo);
        return m;
    }

    @Test
    @SuppressWarnings("unchecked") // o captor de List<...> é inevitavelmente genérico
    void respondeComOContratoEGravaPerguntaERespostaJuntas() {
        MensagemChatResponse resposta = service.perguntar(documentoId.toString(), "  Qual é a multa?  ", usuario);

        verify(servicoDeIA).conversar("texto do contrato", List.of(), "Qual é a multa?");

        ArgumentCaptor<List<MensagemChat>> gravadas = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(gravadas.capture());
        assertEquals(2, gravadas.getValue().size());
        assertEquals("user", gravadas.getValue().get(0).getPapel());
        assertEquals("Qual é a multa?", gravadas.getValue().get(0).getConteudo());
        assertEquals("assistant", gravadas.getValue().get(1).getPapel());

        // Só a resposta da IA volta, no formato que o frontend valida.
        assertEquals("assistant", resposta.role());
        assertEquals("A multa é de 10%.", resposta.content());
        assertEquals(documentoId.toString(), resposta.documentId());
        assertEquals("2", resposta.id());
    }

    @Test
    void historicoVaiParaAIaDaMaisAntigaParaAMaisRecente() {
        // O repositório devolve as mais recentes primeiro.
        when(repository.findByDocumentoIdOrderByIdDesc(eq(documentoId), any(Pageable.class))).thenReturn(List.of(
                mensagem(4, "assistant", "resposta 2"),
                mensagem(3, "user", "pergunta 2"),
                mensagem(2, "assistant", "resposta 1"),
                mensagem(1, "user", "pergunta 1")));

        service.perguntar(documentoId.toString(), "pergunta 3", usuario);

        verify(servicoDeIA).conversar("texto do contrato", List.of(
                new TurnoDeConversa("user", "pergunta 1"),
                new TurnoDeConversa("assistant", "resposta 1"),
                new TurnoDeConversa("user", "pergunta 2"),
                new TurnoDeConversa("assistant", "resposta 2")), "pergunta 3");
    }

    @Test
    void pedeAoBancoSoAsDezMensagensMaisRecentes() {
        service.perguntar(documentoId.toString(), "oi", usuario);

        ArgumentCaptor<Pageable> limite = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByDocumentoIdOrderByIdDesc(eq(documentoId), limite.capture());
        assertEquals(10, limite.getValue().getPageSize());
        assertEquals(0, limite.getValue().getPageNumber());
    }

    @Test
    void perguntaEmBrancoOuNulaEhRecusadaAntesDeChamarAIa() {
        assertThrows(ContratoInvalidoException.class, () -> service.perguntar(documentoId.toString(), "   ", usuario));
        assertThrows(ContratoInvalidoException.class, () -> service.perguntar(documentoId.toString(), null, usuario));

        verifyNoInteractions(servicoDeIA);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void perguntaAcimaDe2000CaracteresEhRecusada() {
        assertThrows(ContratoInvalidoException.class,
                () -> service.perguntar(documentoId.toString(), "a".repeat(2001), usuario));

        service.perguntar(documentoId.toString(), "a".repeat(2000), usuario); // exatamente no limite passa
    }

    @Test
    void falhaDaIaNaoGravaNada() {
        when(servicoDeIA.conversar(anyString(), anyList(), anyString()))
                .thenThrow(new ServicoExternoException("IA (Gemini)", null));

        assertThrows(ServicoExternoException.class, () -> service.perguntar(documentoId.toString(), "oi", usuario));

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void respostaVaziaDaIaContaComoFalhaENaoGravaNada() {
        when(servicoDeIA.conversar(anyString(), anyList(), anyString())).thenReturn("   ");

        assertThrows(ServicoExternoException.class, () -> service.perguntar(documentoId.toString(), "oi", usuario));

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void documentoInexistenteLancaNaoEncontradoSemChamarAIa() {
        when(documentoService.obterDoDono(eq("nao-existe"), any())).thenThrow(new DocumentoNaoEncontradoException());
        when(documentoService.obterParaLeitura(eq("nao-existe"), any())).thenThrow(new DocumentoNaoEncontradoException());

        assertThrows(DocumentoNaoEncontradoException.class, () -> service.perguntar("nao-existe", "oi", usuario));
        assertThrows(DocumentoNaoEncontradoException.class, () -> service.historico("nao-existe", usuario));

        verifyNoInteractions(servicoDeIA);
    }

    @Test
    void historicoDevolveAConversaNaOrdemEmFormatoDoFrontend() {
        when(repository.findByDocumentoIdOrderByIdAsc(documentoId)).thenReturn(List.of(
                mensagem(1, "user", "Qual é a multa?"),
                mensagem(2, "assistant", "10%.")));

        List<MensagemChatResponse> historico = service.historico(documentoId.toString(), usuario);

        assertEquals(2, historico.size());
        assertEquals("user", historico.get(0).role());
        assertEquals("assistant", historico.get(1).role());
        assertEquals(documentoId.toString(), historico.get(0).documentId());
        assertEquals("1", historico.get(0).id());
    }
}
