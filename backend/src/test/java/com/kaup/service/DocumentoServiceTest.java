package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.DocumentoRepository;

class DocumentoServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private AnaliseService analiseService;
    private ServicoDeIA servicoDeIA;
    private DocumentoRepository repository;
    private DocumentoService service;

    @BeforeEach
    void preparar() {
        analiseService = mock(AnaliseService.class);
        servicoDeIA = mock(ServicoDeIA.class);
        repository = mock(DocumentoRepository.class);
        service = new DocumentoService(analiseService, servicoDeIA, repository, mapper);
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
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());

        DocumentoResponse resposta = service.criar(contrato, "Casa");

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
        assertThrows(ContratoInvalidoException.class, () -> service.criar(arquivo("c.pdf"), "Inexistente"));

        verifyNoInteractions(analiseService);
        verify(repository, never()).save(any());
    }

    @Test
    void categoriaEscolhidaPeloUsuarioNaoChamaAClassificacao() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());

        assertEquals("Carros", service.criar(contrato, "Carros").categoryId());

        verifyNoInteractions(servicoDeIA);
    }

    @Test
    void semCategoriaAIaClassifica() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenReturn("Casa");

        assertEquals("Casa", service.criar(contrato, null).categoryId());
        assertEquals("Casa", service.criar(contrato, "  ").categoryId()); // em branco = sem categoria
    }

    @Test
    void respostaDaIaEmMinusculaEhNormalizada() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenReturn(" banco ");

        assertEquals("Banco", service.criar(contrato, null).categoryId());
    }

    @Test
    void respostaDaIaForaDaListaOuVaziaViraOutros() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());

        when(servicoDeIA.classificar(any(), any())).thenReturn("Viagens");
        assertEquals("Outros", service.criar(contrato, null).categoryId());

        when(servicoDeIA.classificar(any(), any())).thenReturn("");
        assertEquals("Outros", service.criar(contrato, null).categoryId());
    }

    @Test
    void falhaNaClassificacaoNaoDerrubaOUploadEVaiParaOutros() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenThrow(new ServicoExternoException("IA (Gemini)", null));

        DocumentoResponse resposta = service.criar(contrato, null);

        assertEquals("Outros", resposta.categoryId());
        verify(repository).save(any(Documento.class)); // o documento analisado não se perde
    }

    @Test
    void aClassificacaoRecebeOTextoLidoEAsCategoriasConhecidas() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenReturn("Casa");

        service.criar(contrato, null);

        verify(servicoDeIA).classificar("texto lido do contrato", Categorias.DESCRICOES);
    }

    @Test
    void falhaNaAnaliseNaoSalvaNada() throws Exception {
        MockMultipartFile contrato = arquivo("contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenThrow(new ContratoInvalidoException("ilegível"));

        assertThrows(ContratoInvalidoException.class, () -> service.criar(contrato, null));

        verify(repository, never()).save(any());
    }

    @Test
    void nomeDoArquivoPerdeOCaminhoDoCliente() throws Exception {
        MockMultipartFile contrato = arquivo("C:\\Users\\fulano\\contrato.pdf");
        when(analiseService.analisarArquivo(contrato)).thenReturn(resultado());

        assertEquals("contrato.pdf", service.criar(contrato, null).name());
    }

    @Test
    void idQueNaoEUuidEhTratadoComoNaoEncontrado() {
        assertThrows(DocumentoNaoEncontradoException.class, () -> service.buscar("abc"));

        verifyNoInteractions(repository);
    }

    @Test
    void idInexistenteLancaNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DocumentoNaoEncontradoException.class, () -> service.analise(id.toString()));
    }

    @Test
    void analiseDevolveResumoEPontosDoDocumentoSalvo() throws Exception {
        UUID id = UUID.randomUUID();
        Documento documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", id);
        documento.setProcessingStatus("completed");
        documento.setAnaliseJson(resultado().analise().toString());
        when(repository.findById(id)).thenReturn(Optional.of(documento));

        AnaliseResponse analise = service.analise(id.toString());

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
        documento.setAnaliseJson("{isto não é json");
        when(repository.findById(id)).thenReturn(Optional.of(documento));

        AnaliseResponse analise = service.analise(id.toString());

        assertNull(analise.summary());
        assertEquals(0, analise.keyPoints().size());
    }
}
