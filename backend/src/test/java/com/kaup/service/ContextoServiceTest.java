package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.kaup.dto.AnexoResponse;
import com.kaup.entity.AnexoContexto;
import com.kaup.entity.Documento;
import com.kaup.entity.Papel;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.repository.AnexoContextoRepository;
import com.kaup.security.UsuarioLogado;

class ContextoServiceTest {

    private final UUID documentoId = UUID.randomUUID();
    private final UsuarioLogado usuario = new UsuarioLogado(UUID.randomUUID(), "ana@exemplo.com", Papel.USER, null);
    private DocumentoService documentoService;
    private AnaliseService analiseService;
    private AnexoContextoRepository repository;
    private ContextoService service;

    @BeforeEach
    void preparar() throws Exception {
        documentoService = mock(DocumentoService.class);
        analiseService = mock(AnaliseService.class);
        repository = mock(AnexoContextoRepository.class);
        service = new ContextoService(documentoService, analiseService, repository);

        Documento documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", documentoId);
        when(documentoService.obterDoDono(any(), any())).thenReturn(documento);
        when(documentoService.obterParaLeitura(any(), any())).thenReturn(documento);
        when(analiseService.extrairTexto(any())).thenReturn("multa passa a 5%");
        when(repository.save(any(AnexoContexto.class))).thenAnswer(i -> {
            AnexoContexto a = i.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 9L);
            return a;
        });
    }

    private MockMultipartFile arquivo(String nome) {
        return new MockMultipartFile("file", nome, "application/pdf", "x".getBytes());
    }

    @Test
    void guardaOTextoLidoESemCaminhoNoNome() throws Exception {
        AnexoResponse resposta = service.anexar(documentoId.toString(), arquivo("C:\\pasta\\aditivo.pdf"), usuario);

        ArgumentCaptor<AnexoContexto> salvo = ArgumentCaptor.forClass(AnexoContexto.class);
        verify(repository).save(salvo.capture());
        assertEquals("aditivo.pdf", salvo.getValue().getNome());
        assertEquals("multa passa a 5%", salvo.getValue().getTextoExtraido());
        assertEquals("9", resposta.id());
        assertEquals(documentoId.toString(), resposta.documentId());
    }

    @Test
    void cortaOTextoQueExcedeOLimite() throws Exception {
        when(analiseService.extrairTexto(any())).thenReturn("a".repeat(ContextoService.TEXTO_MAX_POR_ANEXO + 500));

        service.anexar(documentoId.toString(), arquivo("grande.pdf"), usuario);

        ArgumentCaptor<AnexoContexto> salvo = ArgumentCaptor.forClass(AnexoContexto.class);
        verify(repository).save(salvo.capture());
        assertEquals(ContextoService.TEXTO_MAX_POR_ANEXO, salvo.getValue().getTextoExtraido().length());
    }

    @Test
    void recusaQuandoJaHaAnexosDemais() throws Exception {
        when(repository.countByDocumentoId(documentoId)).thenReturn((long) ContextoService.ANEXOS_MAX);

        assertThrows(ContratoInvalidoException.class,
                () -> service.anexar(documentoId.toString(), arquivo("mais.pdf"), usuario));
        verify(analiseService, never()).extrairTexto(any()); // falha antes do OCR
        verify(repository, never()).save(any());
    }

    @Test
    void naoGravaSeOOcrFalhar() throws Exception {
        when(analiseService.extrairTexto(any())).thenThrow(new ContratoInvalidoException("sem texto"));

        assertThrows(ContratoInvalidoException.class,
                () -> service.anexar(documentoId.toString(), arquivo("borrado.pdf"), usuario));
        verify(repository, never()).save(any());
    }
}
