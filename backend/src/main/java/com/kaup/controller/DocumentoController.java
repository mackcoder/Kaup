package com.kaup.controller;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnaliseResponse;
import com.kaup.dto.DocumentoResponse;
import com.kaup.security.UsuarioLogado;
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
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @AuthenticationPrincipal UsuarioLogado usuario) throws IOException {
        DocumentoResponse documento = documentoService.criar(arquivo, categoryId, usuario);
        return ResponseEntity.created(URI.create("/api/documentos/" + documento.id())).body(documento);
    }

    @GetMapping
    public List<DocumentoResponse> listar(@AuthenticationPrincipal UsuarioLogado usuario) {
        return documentoService.listar(usuario);
    }

    @GetMapping("/{id}")
    public DocumentoResponse buscar(@PathVariable String id, @AuthenticationPrincipal UsuarioLogado usuario) {
        return documentoService.buscar(id, usuario);
    }

    @GetMapping("/{id}/analise")
    public AnaliseResponse analise(@PathVariable String id, @AuthenticationPrincipal UsuarioLogado usuario) {
        return documentoService.analise(id, usuario);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id, @AuthenticationPrincipal UsuarioLogado usuario) {
        documentoService.excluir(id, usuario);
    }
}
