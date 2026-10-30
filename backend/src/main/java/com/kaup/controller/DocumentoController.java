package com.kaup.controller;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnaliseResponse;
import com.kaup.dto.DocumentoResponse;
import com.kaup.service.DocumentoService;

/** Contrato REST que o frontend usa (ver frontend/src/services/api/endpoints.ts). */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    /** Recebe o contrato, faz OCR e IA na mesma requisição e devolve o documento já salvo. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> criar(
            @RequestParam("file") MultipartFile arquivo,
            @RequestParam(value = "categoryId", required = false) String categoryId) throws IOException {
        DocumentoResponse documento = documentoService.criar(arquivo, categoryId);
        return ResponseEntity.created(URI.create("/api/documentos/" + documento.id())).body(documento);
    }

    @GetMapping
    public List<DocumentoResponse> listar() {
        return documentoService.listar();
    }

    @GetMapping("/{id}")
    public DocumentoResponse buscar(@PathVariable String id) {
        return documentoService.buscar(id);
    }

    @GetMapping("/{id}/analise")
    public AnaliseResponse analise(@PathVariable String id) {
        return documentoService.analise(id);
    }
}
