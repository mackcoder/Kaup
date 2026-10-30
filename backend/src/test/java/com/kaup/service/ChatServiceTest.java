package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.kaup.dto.MensagemChatResponse;
import com.kaup.entity.Documento;
import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.entity.MensagemChat;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.MensagemChatRepository;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.UsuarioLogado;

class ChatServiceTest {

    private final UUID documentoId = UUID.randomUUID();
    private DocumentoService documentoService;
    private MensagemChatRepository repository;
    private UsuarioRepository usuarios;
    private ServicoDeIA servicoDeIA;
    private ChatService service;
    private Documento documento;
    private final UsuarioLogado usuario = new UsuarioLogado(UUID.randomUUID(), "ana@exemplo.com", Papel.USER, null);

    @BeforeEach
    void preparar() {
        documentoService = mock(DocumentoService.class);
        repository = mock(MensagemChatRepository.class);
        usuarios = mock(UsuarioRepository.class);
        servicoDeIA = mock(ServicoDeIA.class);
        service = new ChatService(documentoService, repository, usuarios, servicoDeIA);

        documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", documentoId);
        documento.setTextoExtraido("texto do contrato");
        when(documentoService.obterDoDono(eq(documentoId.toString()), any())).thenReturn(documento);
        when(documentoService.obterParaLeitura(eq(documentoId.toString()), any())).thenReturn(documento);

        when(repository.findByDocumentoIdOrderByIdDesc(eq(documentoId), any(Pageable.class))).thenReturn(List.of());
        when(servicoDeIA.conversar(anyString(), anyList(), anyString(), any())).thenReturn("  A multa é de 10%.  ");
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

        verify(servicoDeIA).conversar("texto do contrato", List.of(), "Qual é a multa?", PreferenciasDeIA.PADRAO);

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
                new TurnoDeConversa("assistant", "resposta 2")), "pergunta 3", PreferenciasDeIA.PADRAO);
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
        when(servicoDeIA.conversar(anyString(), anyList(), anyString(), any()))
                .thenThrow(new ServicoExternoException("IA (Gemini)", null));

        assertThrows(ServicoExternoException.class, () -> service.perguntar(documentoId.toString(), "oi", usuario));

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void respostaVaziaDaIaContaComoFalhaENaoGravaNada() {
        when(servicoDeIA.conversar(anyString(), anyList(), anyString(), any())).thenReturn("   ");

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

    @Test
    void aIaRecebeOPerfilAtualDeQuemPergunta() {
        Usuario perfil = new Usuario();
        perfil.setExplainLevel("complete");
        perfil.setIncome("3to5");
        perfil.setCompartilharRenda(true);
        when(usuarios.findById(usuario.id())).thenReturn(Optional.of(perfil));

        service.perguntar(documentoId.toString(), "Posso pagar essa parcela?", usuario);

        verify(servicoDeIA).conversar("texto do contrato", List.of(), "Posso pagar essa parcela?",
                new PreferenciasDeIA("complete", "entre R$ 3.000 e R$ 5.000"));
    }

    @Test
    void rendaBloqueadaNaoVaiParaAIa() {
        Usuario perfil = new Usuario();
        perfil.setIncome("more15");
        perfil.setCompartilharRenda(false);
        when(usuarios.findById(usuario.id())).thenReturn(Optional.of(perfil));

        service.perguntar(documentoId.toString(), "oi", usuario);

        verify(servicoDeIA).conversar("texto do contrato", List.of(), "oi", new PreferenciasDeIA("simple", null));
    }

    // ---- bloqueio da renda vale também para o histórico ----

    private MensagemChat respostaComRenda(long id, String conteudo) {
        MensagemChat m = mensagem(id, "assistant", conteudo);
        m.setComRenda(true);
        return m;
    }

    private void perfilComRenda(boolean compartilhar) {
        Usuario perfil = new Usuario();
        perfil.setIncome("7to15");
        perfil.setCompartilharRenda(compartilhar);
        when(usuarios.findById(usuario.id())).thenReturn(Optional.of(perfil));
    }

    @Test
    void comARendaBloqueadaAsRespostasQueUsaramARendaSaemDoHistorico() {
        perfilComRenda(false);
        // Mais recentes primeiro, como o repositório devolve.
        when(repository.findByDocumentoIdOrderByIdDesc(eq(documentoId), any(Pageable.class))).thenReturn(List.of(
                mensagem(6, "assistant", "resposta sem renda"),
                mensagem(5, "user", "pergunta sobre o prazo"),
                respostaComRenda(4, "sua renda é de 7 a 15 mil"),
                mensagem(3, "user", "qual é a minha renda?"),
                mensagem(2, "assistant", "resposta antiga sem renda"),
                mensagem(1, "user", "pergunta antiga")));

        service.perguntar(documentoId.toString(), "e agora?", usuario);

        verify(servicoDeIA).conversar("texto do contrato", List.of(
                new TurnoDeConversa("user", "pergunta antiga"),
                new TurnoDeConversa("assistant", "resposta antiga sem renda"),
                new TurnoDeConversa("user", "pergunta sobre o prazo"),
                new TurnoDeConversa("assistant", "resposta sem renda")), "e agora?",
                new PreferenciasDeIA("simple", null));
    }

    @Test
    void comARendaLiberadaOHistoricoVaiInteiro() {
        perfilComRenda(true);
        when(repository.findByDocumentoIdOrderByIdDesc(eq(documentoId), any(Pageable.class))).thenReturn(List.of(
                respostaComRenda(2, "sua renda é de 7 a 15 mil"),
                mensagem(1, "user", "qual é a minha renda?")));

        service.perguntar(documentoId.toString(), "e agora?", usuario);

        verify(servicoDeIA).conversar("texto do contrato", List.of(
                new TurnoDeConversa("user", "qual é a minha renda?"),
                new TurnoDeConversa("assistant", "sua renda é de 7 a 15 mil")), "e agora?",
                new PreferenciasDeIA("simple", "entre R$ 7.000 e R$ 15.000"));
    }

    @Test
    void mensagensAntigasSemAMarcaContamComoSemRenda() {
        perfilComRenda(false);
        // A coluna é nova: as mensagens anteriores a ela têm valor nulo e não são descartadas.
        when(repository.findByDocumentoIdOrderByIdDesc(eq(documentoId), any(Pageable.class))).thenReturn(List.of(
                mensagem(2, "assistant", "resposta antiga"),
                mensagem(1, "user", "pergunta antiga")));

        service.perguntar(documentoId.toString(), "oi", usuario);

        verify(servicoDeIA).conversar(eq("texto do contrato"), eq(List.of(
                new TurnoDeConversa("user", "pergunta antiga"),
                new TurnoDeConversa("assistant", "resposta antiga"))), eq("oi"), any());
    }

    @Test
    void aRespostaGravadaLembraSeFoiGeradaComARenda() {
        perfilComRenda(true);
        service.perguntar(documentoId.toString(), "oi", usuario);
        perfilComRenda(false);
        service.perguntar(documentoId.toString(), "oi de novo", usuario);

        ArgumentCaptor<List<MensagemChat>> gravadas = ArgumentCaptor.forClass(List.class);
        verify(repository, org.mockito.Mockito.times(2)).saveAll(gravadas.capture());
        assertTrue(gravadas.getAllValues().get(0).get(1).usouRenda());
        assertFalse(gravadas.getAllValues().get(1).get(1).usouRenda());
    }
}
