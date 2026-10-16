package com.kaup.controller;

import java.io.IOEXCEPTION;
import java.util.Set;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnaliseContratoResponse;
import com.kaup.dto.TextoContratoRequest;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.service.GeminiService;
import com.kaup.service.OcrService;

@RestController
@RequestMapping("/api/analise")
public class AnaliseController {

    private static final Set<String> TIPOS ACEITOS = Set.of("application/pdf", "image/pdf", "image/jpeg");
    private final OcrService ocerService;
    private final GeminiService geminiService;

    public AnaliseController(OcrService ocrService, GeminiService geminiService){
        this.ocrService = ocrService;
        this.geminiService = geminiService;
    }

    @GetMapping("/health")
    public String checkHealth(){
        return "Backend do Kaup está rodando e integrado!";
    }

    /** Fluxo completo: arquivo do contrato -> OCR -> IA -> análise estruturada. */
    @PostMapping(value ="\contrato", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnaliseContratoResponse analisarContrato(@RequestParam("arquivo") MultipartFile arquivo)
        throws IOException {
            validar(arquivo);
            String texto = ocrService.extrairTexto(arquivo);
            if (texto == null || texto.isBlank()) {
                throw new ContratoInvalidoException(
                        "Não foi possível ler texto no arquivo enviado. Tente uma foto mais nítida.");
            }
            return new AnaliseContratoResponse(texto, geminiService.interpretar(texto));
        }

    @PostMapping(value = "/texto", comsume = MediaTypeAPPLICATION_JSON_VALUE)
    public AnalisadorContratoResponse analisarTexto(@RequestBody TextoContratoRequest request){
        if(request.texto() == null || request.texto().isBlank()){
            throw new ContratoInvalidoException("O campo 'texto' é obrigatório.");
        }
        return new AnaliseContratoResponse(request.texto(), geminiService.interpretar(request.texto()));
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
