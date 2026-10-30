package com.kaup.service;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnaliseContratoResponse;
import com.kaup.exception.ContratoInvalidoException;

/** Fluxo de análise de um contrato: validação -> OCR -> IA. Não grava nada. */
@Service
public class AnaliseService {

    private static final Set<String> TIPOS_ACEITOS = Set.of(
            "application/pdf", "image/png", "image/jpeg");

    private final OcrService ocrService;
    private final ServicoDeIA servicoDeIA;

    public AnaliseService(OcrService ocrService, ServicoDeIA servicoDeIA) {
        this.ocrService = ocrService;
        this.servicoDeIA = servicoDeIA;
    }

    /** Arquivo do contrato -> OCR -> IA -> análise estruturada. */
    public AnaliseContratoResponse analisarArquivo(MultipartFile arquivo, PreferenciasDeIA preferencias)
            throws IOException {
        validar(arquivo);
        String texto = ocrService.extrairTexto(arquivo);
        if (texto == null || texto.isBlank()) {
            throw new ContratoInvalidoException(
                    "Não foi possível ler texto no arquivo enviado. Tente uma foto mais nítida.");
        }
        return new AnaliseContratoResponse(texto, servicoDeIA.interpretar(texto, preferencias));
    }

    /** Mesmo fluxo a partir de texto já extraído (pula o OCR). */
    public AnaliseContratoResponse analisarTexto(String texto, PreferenciasDeIA preferencias) {
        if (texto == null || texto.isBlank()) {
            throw new ContratoInvalidoException("O campo 'texto' é obrigatório.");
        }
        return new AnaliseContratoResponse(texto, servicoDeIA.interpretar(texto, preferencias));
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ContratoInvalidoException("Nenhum arquivo foi enviado.");
        }
        String tipo = arquivo.getContentType();
        // Set.of(...).contains(null) lança NullPointerException, por isso a checagem de null
        if (tipo == null || !TIPOS_ACEITOS.contains(tipo)) {
            throw new ContratoInvalidoException(
                    "Formato não suportado. Envie PDF, PNG ou JPEG.");
        }
    }
}
