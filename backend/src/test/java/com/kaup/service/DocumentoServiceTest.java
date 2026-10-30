package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
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
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.UsuarioLogado;
import com.kaup.exception.OperacaoNaoPermitidaException;

class DocumentoServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private AnaliseService analiseService;
    private ServicoDeIA servicoDeIA;
    private DocumentoRepository repository;
    private UsuarioRepository usuarios;
    private final UsuarioLogado usuario = new UsuarioLogado(UUID.randomUUID(), "ana@exemplo.com", Papel.USER, null);
    private final UsuarioLogado admin = new UsuarioLogado(UUID.randomUUID(), "admin@admin", Papel.ADMIN, null);
    private DocumentoService service;

    @BeforeEach
    void preparar() {
        analiseService = mock(AnaliseService.class);
        servicoDeIA = mock(ServicoDeIA.class);
        repository = mock(DocumentoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        service = new DocumentoService(analiseService, servicoDeIA, repository, usuarios, mapper);
        // Simula o banco: save devolve a mesma entidade, já com id.
        when(repository.save(any(Documento.class))).thenAnswer(invocacao -> {
            Documento documento = invocacao.getArgument(0);
            ReflectionTestUtils.setField(documento, "id", UUID.randomUUID());
            return documento;
        });
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
    void criaDocumentoComDadosDaAnalise() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        DocumentoResponse resposta = service.criar(contrato, "Casa", usuario);

        ArgumentCaptor<Documento> salvo = ArgumentCaptor.forClass(Documento.class);
        verify(repository).save(salvo.capture());
        assertEquals("texto lido do contrato", salvo.getValue().getTextoExtraido());
        assertEquals("application/pdf", salvo.getValue().getMimeType());
        assertEquals("contrato.pdf", resposta.name());
        assertEquals("Casa", resposta.categoryId());
        assertEquals("completed", resposta.processingStatus());
        assertEquals("Contrato de aluguel.", resposta.description());
        assertEquals("warning", resposta.visualStatus());
        UUID.fromString(resposta.id()); // o id é um UUID válido, em texto
    }

    @Test
    void categoriaInvalidaFalhaAntesDoOcrENaoSalva() {
        assertThrows(ContratoInvalidoException.class, () -> service.criar(arquivo("c.pdf"), "Inexistente", usuario));

        verifyNoInteractions(analiseService);
        verify(repository, never()).save(any());
    }

    @Test
    void categoriaEscolhidaPeloUsuarioNaoChamaAClassificacao() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        assertEquals("Carros", service.criar(contrato, "Carros", usuario).categoryId());

        verifyNoInteractions(servicoDeIA);
    }

    @Test
    void semCategoriaAIaClassifica() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenReturn("Casa");

        assertEquals("Casa", service.criar(contrato, null, usuario).categoryId());
        assertEquals("Casa", service.criar(contrato, "  ", usuario).categoryId()); // em branco = sem categoria
    }

    @Test
    void respostaDaIaEmMinusculaEhNormalizada() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenReturn(" banco ");

        assertEquals("Banco", service.criar(contrato, null, usuario).categoryId());
    }

    @Test
    void respostaDaIaForaDaListaOuVaziaViraOutros() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        when(servicoDeIA.classificar(any(), any())).thenReturn("Viagens");
        assertEquals("Outros", service.criar(contrato, null, usuario).categoryId());

        when(servicoDeIA.classificar(any(), any())).thenReturn("");
        assertEquals("Outros", service.criar(contrato, null, usuario).categoryId());
    }

    @Test
    void falhaNaClassificacaoNaoDerrubaOUploadEVaiParaOutros() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenThrow(new ServicoExternoException("IA (Gemini)", null));

        DocumentoResponse resposta = service.criar(contrato, null, usuario);

        assertEquals("Outros", resposta.categoryId());
        verify(repository).save(any(Documento.class)); // o documento analisado não se perde
    }

    @Test
    void aClassificacaoRecebeOTextoLidoEAsCategoriasConhecidas() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenReturn("Casa");

        service.criar(contrato, null, usuario);

        verify(servicoDeIA).classificar("texto lido do contrato", Categorias.DESCRICOES);
    }

    @Test
    void falhaNaAnaliseNaoSalvaNada() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenThrow(new ContratoInvalidoException("ilegível"));

        assertThrows(ContratoInvalidoException.class, () -> service.criar(contrato, null, usuario));

        verify(repository, never()).save(any());
    }

    @Test
    void nomeDoArquivoPerdeOCaminhoDoCliente() throws Exception {
        MockMultipartFile contrato = arquivo("C:\\Users\\fulano\\contrato.pdf");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        assertEquals("contrato.pdf", service.criar(contrato, null, usuario).name());
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
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());
        Usuario referencia = new Usuario();
        when(usuarios.getReferenceById(usuario.id())).thenReturn(referencia);

        service.criar(contrato, "Casa", usuario);

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
    void aAnaliseUsaOPerfilDeQuemEnvia() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        Usuario perfil = new Usuario();
        perfil.setExplainLevel("complete");
        perfil.setIncome("3to5");
        when(usuarios.findById(usuario.id())).thenReturn(Optional.of(perfil));
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        service.criar(contrato, "Casa", usuario);

        // A renda existe no perfil, mas o serviço a repassa à camada de IA, que decide não usá-la na análise.
        verify(analiseService).analisarArquivo(contrato, new PreferenciasDeIA("complete", "entre R$ 3.000 e R$ 5.000"));
    }

    @Test
    void ownedByMeDizSeODocumentoEDeQuemConsulta() {
        Documento meu = documentoDe(usuario.id());
        Documento alheio = documentoDe(UUID.randomUUID());

        assertTrue(service.buscar(meu.getId().toString(), usuario).ownedByMe());
        // O administrador lê o documento de outra pessoa, mas não é o dono dele.
        assertFalse(service.buscar(alheio.getId().toString(), admin).ownedByMe());
    }
}
