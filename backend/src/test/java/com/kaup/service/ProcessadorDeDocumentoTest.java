package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaup.dto.AnaliseContratoResponse;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.DocumentoRepository;

class ProcessadorDeDocumentoTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID id = UUID.randomUUID();
    private final MockMultipartFile contrato =
            new MockMultipartFile("file", "contrato.pdf", "application/pdf", "conteudo".getBytes());
    private AnaliseService analiseService;
    private ServicoDeIA servicoDeIA;
    private DocumentoRepository repository;
    private ProcessadorDeDocumento processador;

    @BeforeEach
    void preparar() {
        analiseService = mock(AnaliseService.class);
        servicoDeIA = mock(ServicoDeIA.class);
        repository = mock(DocumentoRepository.class);
        processador = new ProcessadorDeDocumento(analiseService, servicoDeIA, repository);
        when(repository.marcarProcessando(id)).thenReturn(1);
    }

    private AnaliseContratoResponse resultado() throws Exception {
        return new AnaliseContratoResponse("texto lido do contrato", mapper.readTree("""
                {"resumo": "Contrato de aluguel. Define o valor.", "pontosDeAtencao": ["Multa"]}"""));
    }

    @Test
    void leOArquivoEConcluiComTextoEAnalise() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        processador.processar(id, contrato, "Casa", PreferenciasDeIA.PADRAO);

        ArgumentCaptor<String> analise = ArgumentCaptor.forClass(String.class);
        verify(repository).marcarProcessando(id);
        verify(repository).concluir(eq(id), eq("Casa"), eq("texto lido do contrato"), analise.capture());
        assertEquals("Contrato de aluguel. Define o valor.", mapper.readTree(analise.getValue()).path("resumo").asText());
        verify(repository, never()).falhar(any(), any());
        verifyNoInteractions(servicoDeIA); // categoria escolhida: sem classificação
    }

    @Test
    void usaOPerfilRecebido() throws Exception {
        PreferenciasDeIA perfil = new PreferenciasDeIA("complete", "entre R$ 3.000 e R$ 5.000");
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        processador.processar(id, contrato, "Casa", perfil);

        // A renda segue para a camada de IA, que decide não usá-la na análise.
        verify(analiseService).analisarArquivo(contrato, perfil);
    }

    @Test
    void semCategoriaAIaClassificaComOTextoLido() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenReturn(" banco ");

        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);

        verify(servicoDeIA).classificar("texto lido do contrato", Categorias.DESCRICOES);
        verify(repository).concluir(eq(id), eq("Banco"), any(), any()); // normalizada
    }

    @Test
    void respostaDaIaForaDaListaOuVaziaViraOutros() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());

        when(servicoDeIA.classificar(any(), any())).thenReturn("Viagens");
        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);
        when(servicoDeIA.classificar(any(), any())).thenReturn("");
        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);

        verify(repository, org.mockito.Mockito.times(2)).concluir(eq(id), eq("Outros"), any(), any());
    }

    @Test
    void falhaNaClassificacaoNaoDerrubaAAnalise() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any())).thenReturn(resultado());
        when(servicoDeIA.classificar(any(), any())).thenThrow(new ServicoExternoException("IA (Gemini)", null));

        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);

        verify(repository).concluir(eq(id), eq("Outros"), any(), any());
        verify(repository, never()).falhar(any(), any());
    }

    @Test
    void contratoIlegivelFalhaComAMensagemParaAPessoa() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any()))
                .thenThrow(new ContratoInvalidoException("O PDF está protegido por senha."));

        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);

        verify(repository).falhar(id, "O PDF está protegido por senha.");
        verify(repository, never()).concluir(any(), any(), any(), any());
    }

    @Test
    void iaForaDoArFalhaComMensagemSemDetalheTecnico() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any()))
                .thenThrow(new ServicoExternoException("IA (Gemini)", new RuntimeException("chave 123 inválida")));

        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);

        verify(repository).falhar(id, ProcessadorDeDocumento.FALHA_IA);
    }

    @Test
    void erroInesperadoFalhaComMensagemGenerica() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any())).thenThrow(new IllegalStateException("boom"));

        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);

        verify(repository).falhar(id, ProcessadorDeDocumento.FALHA_GENERICA);
    }

    @Test
    void documentoApagadoNaFilaNaoEhLido() throws Exception {
        when(repository.marcarProcessando(id)).thenReturn(0);

        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO);

        verifyNoInteractions(analiseService, servicoDeIA);
        verify(repository, never()).concluir(any(), any(), any(), any());
        verify(repository, never()).falhar(any(), any());
    }

    @Test
    void falhaAoGravarAFalhaNaoPropagaParaOExecutor() throws Exception {
        when(analiseService.analisarArquivo(eq(contrato), any())).thenThrow(new IllegalStateException("boom"));
        when(repository.falhar(any(), any())).thenThrow(new IllegalStateException("banco fora do ar"));

        processador.processar(id, contrato, null, PreferenciasDeIA.PADRAO); // não lança
    }
}
