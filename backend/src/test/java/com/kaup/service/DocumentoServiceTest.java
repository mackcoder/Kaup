package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaup.dto.AnaliseContratoResponse;
import com.kaup.dto.AnaliseResponse;
import com.kaup.dto.DocumentoResponse;
import com.kaup.entity.Documento;
import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.exception.FilaCheiaException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.UsuarioLogado;
import com.kaup.exception.OperacaoNaoPermitidaException;

class DocumentoServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private AnaliseService analiseService;
    private ServicoDeIA servicoDeIA;
    private ProcessadorDeDocumento processador;
    // O executor de mentira só guarda as tarefas: cada teste decide se (e quando) elas rodam.
    private final List<Runnable> tarefas = new ArrayList<>();
    private Executor executor;
    private DocumentoRepository repository;
    private UsuarioRepository usuarios;
    private final UsuarioLogado usuario = new UsuarioLogado(UUID.randomUUID(), "ana@exemplo.com", Papel.USER, null);
    private final UsuarioLogado admin = new UsuarioLogado(UUID.randomUUID(), "admin@admin", Papel.ADMIN, null);
    private TetoDiarioGemini teto;
    private DocumentoService service;

    @BeforeEach
    void preparar() {
        analiseService = mock(AnaliseService.class);
        servicoDeIA = mock(ServicoDeIA.class);
        repository = mock(DocumentoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        teto = mock(TetoDiarioGemini.class);
        processador = mock(ProcessadorDeDocumento.class);
        executor = tarefas::add;
        service = novoService();
        // Simula o banco: save devolve a mesma entidade, já com id.
        when(repository.save(any(Documento.class))).thenAnswer(invocacao -> {
            Documento documento = invocacao.getArgument(0);
            ReflectionTestUtils.setField(documento, "id", UUID.randomUUID());
            return documento;
        });
    }

    private DocumentoService novoService() {
        return new DocumentoService(analiseService, processador, executor, repository, usuarios, mapper, teto);
    }

    private MockMultipartFile arquivo(String nome) {
        return new MockMultipartFile("file", nome, "application/pdf", "conteudo".getBytes());
    }

    private AnaliseContratoResponse resultado() throws Exception {
        return new AnaliseContratoResponse("texto lido do contrato", mapper.readTree("""
                {"resumo": "Contrato de aluguel. Define o valor.",
                 "valores": [{"descricao": "Aluguel", "valor": "R$ 1.500,00"}],
                 "pontosDeAtencao": ["Multa na rescisão"]}"""));
    }

    @Test
    void criarGravaPendenteEEntregaALeituraAoExecutorSemEsperar() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");

        DocumentoResponse resposta = service.criar(contrato, "Casa", null, usuario);

        ArgumentCaptor<Documento> salvo = ArgumentCaptor.forClass(Documento.class);
        verify(repository).save(salvo.capture());
        assertEquals("pending", salvo.getValue().getProcessingStatus());
        assertEquals("application/pdf", salvo.getValue().getMimeType());
        assertNull(salvo.getValue().getTextoExtraido()); // ainda não foi lido
        assertEquals("contrato.pdf", resposta.name());
        assertEquals("Casa", resposta.categoryId());
        assertEquals("pending", resposta.processingStatus());
        assertEquals("pending", resposta.visualStatus());
        assertNull(resposta.description());
        assertTrue(resposta.ownedByMe()); // quem enviou é o dono, mesmo antes de o banco devolver o donoId
        UUID.fromString(resposta.id()); // o id é um UUID válido, em texto
        // O executor ficou com a tarefa; nada de OCR nem de IA aconteceu na requisição.
        assertEquals(1, tarefas.size());
        verifyNoInteractions(processador, servicoDeIA);
        verify(analiseService, never()).analisarArquivo(any(), any());
    }

    @Test
    void aTarefaEntregueAoExecutorLeOArquivoCopiadoComACategoriaEOPerfil() throws Exception {
        Usuario perfil = new Usuario();
        perfil.setExplainLevel("complete");
        when(usuarios.findById(usuario.id())).thenReturn(Optional.of(perfil));

        DocumentoResponse resposta = service.criar(arquivo("c.pdf"), "Carros", null, usuario);
        tarefas.get(0).run();

        ArgumentCaptor<MultipartFile> copia = ArgumentCaptor.forClass(MultipartFile.class);
        verify(processador).processar(eq(UUID.fromString(resposta.id())), copia.capture(),
                eq("Carros"), eq(PreferenciasDeIA.de(perfil)));
        assertEquals("c.pdf", copia.getValue().getOriginalFilename());
        assertEquals("conteudo", new String(copia.getValue().getBytes()));
    }

    @Test
    void semCategoriaAtarefaRecebeNuloEAIaClassifica() throws Exception {
        service.criar(arquivo("c.pdf"), "  ", null, usuario); // em branco = sem categoria
        tarefas.get(0).run();

        verify(processador).processar(any(), any(), isNull(), any());
    }

    @Test
    void categoriaInvalidaFalhaAntesDeLerOArquivoENaoSalva() {
        assertThrows(ContratoInvalidoException.class, () -> service.criar(arquivo("c.pdf"), "Inexistente", null, usuario));

        verifyNoInteractions(analiseService);
        verify(repository, never()).save(any());
        assertTrue(tarefas.isEmpty());
    }

    @Test
    void arquivoInvalidoEhRecusadoNaHoraENaoSalva() {
        doThrow(new ContratoInvalidoException("Formato não suportado.")).when(analiseService).validar(any());

        assertThrows(ContratoInvalidoException.class, () -> service.criar(arquivo("c.pdf"), null, null, usuario));

        verify(repository, never()).save(any());
        assertTrue(tarefas.isEmpty());
    }

    @Test
    void filaCheiaApagaOPendenteEResponde429() {
        executor = tarefa -> { throw new RejectedExecutionException("cheia"); };
        service = novoService();

        assertThrows(FilaCheiaException.class, () -> service.criar(arquivo("c.pdf"), null, null, usuario));

        ArgumentCaptor<Documento> salvo = ArgumentCaptor.forClass(Documento.class);
        verify(repository).save(salvo.capture());
        verify(repository).deleteById(salvo.getValue().getId()); // não deixa um "pending" eterno
    }

    @Test
    void chaveJaUsadaDevolveOMesmoDocumentoSemCriarOutro() throws Exception {
        Documento existente = documentoDe(usuario.id());
        existente.setProcessingStatus("pending");
        when(repository.findByDonoIdAndChaveIdempotencia(usuario.id(), "k-1")).thenReturn(Optional.of(existente));

        DocumentoResponse resposta = service.criar(arquivo("c.pdf"), null, " k-1 ", usuario);

        assertEquals(existente.getId().toString(), resposta.id());
        verify(repository, never()).save(any());
        verifyNoInteractions(teto, analiseService);
        assertTrue(tarefas.isEmpty());
    }

    @Test
    void chaveNovaFicaGravadaNoDocumento() throws Exception {
        service.criar(arquivo("c.pdf"), null, "k-2", usuario);

        ArgumentCaptor<Documento> salvo = ArgumentCaptor.forClass(Documento.class);
        verify(repository).save(salvo.capture());
        assertEquals("k-2", salvo.getValue().getChaveIdempotencia());
    }

    @Test
    void doisEnviosSimultaneosComAMesmaChaveFicamComOPrimeiro() throws Exception {
        Documento primeiro = documentoDe(usuario.id());
        when(repository.findByDonoIdAndChaveIdempotencia(usuario.id(), "k-3"))
                .thenReturn(Optional.empty(), Optional.of(primeiro));
        when(repository.save(any(Documento.class))).thenThrow(new DataIntegrityViolationException("chave"));

        DocumentoResponse resposta = service.criar(arquivo("c.pdf"), null, "k-3", usuario);

        assertEquals(primeiro.getId().toString(), resposta.id());
        assertTrue(tarefas.isEmpty());
    }

    @Test
    void chaveGrandeDemaisEhRecusada() {
        assertThrows(ContratoInvalidoException.class,
                () -> service.criar(arquivo("c.pdf"), null, "x".repeat(65), usuario));
    }

    @Test
    void documentoNaoLidoAindaNaoTemTextoNemAnaliseMasNaoQuebra() {
        Documento pendente = documentoDe(usuario.id());
        pendente.setProcessingStatus("pending");
        pendente.setAnaliseJson(null);

        DocumentoResponse resposta = service.buscar(pendente.getId().toString(), usuario);
        AnaliseResponse analise = service.analise(pendente.getId().toString(), usuario);

        assertEquals("pending", resposta.processingStatus());
        assertEquals("pending", resposta.visualStatus());
        assertEquals("pending", analise.processingStatus());
        assertNull(analise.summary());
        assertEquals(0, analise.keyPoints().size());
    }

    @Test
    void documentoFalhoMostraOMotivoNaListaENaAnalise() {
        Documento falho = documentoDe(usuario.id());
        falho.setProcessingStatus("failed");
        falho.setAnaliseJson(null);
        falho.setFalha("O PDF está protegido por senha.");

        assertEquals("O PDF está protegido por senha.",
                service.buscar(falho.getId().toString(), usuario).failureReason());
        assertEquals("O PDF está protegido por senha.",
                service.analise(falho.getId().toString(), usuario).ocrQualityWarning());
    }

    @Test
    void exigirConcluidoRecusaPendenteProcessandoEFalho() {
        Documento documento = new Documento();
        for (String status : List.of("pending", "processing", "failed")) {
            documento.setProcessingStatus(status);
            assertThrows(ContratoInvalidoException.class, () -> service.exigirConcluido(documento));
        }
        documento.setProcessingStatus("completed");
        service.exigirConcluido(documento); // não lança
    }

    @Test
    void nomeDoArquivoPerdeOCaminhoDoCliente() throws Exception {
        MockMultipartFile contrato = arquivo("C:\\Users\\fulano\\contrato.pdf");

        assertEquals("contrato.pdf", service.criar(contrato, null, null, usuario).name());
    }

    @Test
    void idQueNaoEUuidEhTratadoComoNaoEncontrado() {
        assertThrows(DocumentoNaoEncontradoException.class, () -> service.buscar("abc", usuario));

        verifyNoInteractions(repository);
    }

    @Test
    void idInexistenteLancaNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DocumentoNaoEncontradoException.class, () -> service.analise(id.toString(), usuario));
    }

    @Test
    void analiseDevolveResumoEPontosDoDocumentoSalvo() throws Exception {
        UUID id = UUID.randomUUID();
        Documento documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", id);
        documento.setProcessingStatus("completed");
        ReflectionTestUtils.setField(documento, "donoId", usuario.id());
        documento.setAnaliseJson(resultado().analise().toString());
        when(repository.findById(id)).thenReturn(Optional.of(documento));

        AnaliseResponse analise = service.analise(id.toString(), usuario);

        assertEquals(id.toString(), analise.documentId());
        assertEquals("Contrato de aluguel. Define o valor.", analise.summary());
        assertEquals(2, analise.keyPoints().size());
        assertNull(analise.ocrQualityWarning());
    }

    @Test
    void jsonIlegivelNoBancoViraAnaliseVaziaSemQuebrar() {
        UUID id = UUID.randomUUID();
        Documento documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", id);
        documento.setProcessingStatus("completed");
        ReflectionTestUtils.setField(documento, "donoId", usuario.id());
        documento.setAnaliseJson("{isto não é json");
        when(repository.findById(id)).thenReturn(Optional.of(documento));

        AnaliseResponse analise = service.analise(id.toString(), usuario);

        assertNull(analise.summary());
        assertEquals(0, analise.keyPoints().size());
    }

    // ---- quem pode ver e alterar ----

    private Documento documentoDe(UUID dono) {
        Documento documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(documento, "donoId", dono);
        documento.setProcessingStatus("completed");
        documento.setAnaliseJson("{}");
        when(repository.findById(documento.getId())).thenReturn(Optional.of(documento));
        return documento;
    }

    @Test
    void documentoCriadoPertenceAQuemEnviou() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        Usuario referencia = new Usuario();
        when(usuarios.getReferenceById(usuario.id())).thenReturn(referencia);

        service.criar(contrato, "Casa", null, usuario);

        ArgumentCaptor<Documento> salvo = ArgumentCaptor.forClass(Documento.class);
        verify(repository).save(salvo.capture());
        assertEquals(referencia, salvo.getValue().getDono());
    }

    @Test
    void usuarioComumListaSoOsProprios() {
        Documento meu = documentoDe(usuario.id());
        when(repository.findByDonoIdOrderByCriadoEmDesc(usuario.id())).thenReturn(List.of(meu));

        List<DocumentoResponse> lista = service.listar(usuario);

        assertEquals(1, lista.size());
        verify(repository, never()).findAllByOrderByCriadoEmDesc();
    }

    @Test
    void administradorListaOsDocumentosDeTodos() {
        Documento deUm = documentoDe(UUID.randomUUID());
        Documento deOutro = documentoDe(UUID.randomUUID());
        when(repository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(deUm, deOutro));

        assertEquals(2, service.listar(admin).size());
        verify(repository, never()).findByDonoIdOrderByCriadoEmDesc(any());
    }

    @Test
    void documentoDeOutraPessoaParaUsuarioComumNaoExiste() {
        Documento alheio = documentoDe(UUID.randomUUID());

        assertThrows(DocumentoNaoEncontradoException.class, () -> service.buscar(alheio.getId().toString(), usuario));
        assertThrows(DocumentoNaoEncontradoException.class, () -> service.analise(alheio.getId().toString(), usuario));
    }

    @Test
    void documentoSemDonoNaoApareceParaUsuarioComum() {
        Documento orfao = documentoDe(null);

        assertThrows(DocumentoNaoEncontradoException.class, () -> service.buscar(orfao.getId().toString(), usuario));
    }

    @Test
    void administradorLeDocumentoDeOutraPessoa() {
        Documento alheio = documentoDe(UUID.randomUUID());

        assertEquals(alheio.getId().toString(), service.buscar(alheio.getId().toString(), admin).id());
    }

    @Test
    void donoApagaOProprioDocumento() {
        Documento meu = documentoDe(usuario.id());

        service.excluir(meu.getId().toString(), usuario);

        verify(repository).delete(meu);
    }

    @Test
    void ninguemApagaDocumentoAlheio() {
        Documento alheio = documentoDe(UUID.randomUUID());

        assertThrows(DocumentoNaoEncontradoException.class, () -> service.excluir(alheio.getId().toString(), usuario));
        // O administrador lê tudo, mas não altera o que é dos outros, e fica sabendo por quê.
        assertThrows(OperacaoNaoPermitidaException.class, () -> service.excluir(alheio.getId().toString(), admin));

        verify(repository, never()).delete(any(Documento.class));
    }

    @Test
    void ownedByMeDizSeODocumentoEDeQuemConsulta() {
        Documento meu = documentoDe(usuario.id());
        Documento alheio = documentoDe(UUID.randomUUID());

        assertTrue(service.buscar(meu.getId().toString(), usuario).ownedByMe());
        // O administrador lê o documento de outra pessoa, mas não é o dono dele.
        assertFalse(service.buscar(alheio.getId().toString(), admin).ownedByMe());
    }

    @Test
    void comOTetoDiarioEstouradoNaoEnfileiraNemGrava() throws Exception {
        doThrow(new com.kaup.exception.LimiteDiarioException(60)).when(teto).verificar();
        MockMultipartFile contrato = new MockMultipartFile("file", "c.pdf", "application/pdf", "x".getBytes());

        assertThrows(com.kaup.exception.LimiteDiarioException.class, () -> service.criar(contrato, null, null, usuario));

        verify(repository, never()).save(any());
        assertTrue(tarefas.isEmpty());
    }
}
