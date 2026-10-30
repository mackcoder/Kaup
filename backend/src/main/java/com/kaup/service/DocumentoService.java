package com.kaup.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaup.dto.AnaliseContratoResponse;
import com.kaup.dto.AnaliseResponse;
import com.kaup.dto.DocumentoResponse;
import com.kaup.entity.Documento;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.DocumentoNaoEncontradoException;
import com.kaup.repository.DocumentoRepository;

/** Cria e consulta documentos. Traduz entidade e JSON da IA para o formato do frontend. */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    /** Chaves de categoria que o frontend conhece (frontend/src/styles/categories.ts). */
    static final Set<String> CATEGORIAS = Set.of("Carros", "Casa", "Trabalho", "Banco", "Outros");

    static final String STATUS_CONCLUIDO = "completed";

    private static final int TAMANHO_MAX_NOME = 255;

    private final AnaliseService analiseService;
    private final DocumentoRepository repository;
    private final ObjectMapper objectMapper;

    public DocumentoService(AnaliseService analiseService, DocumentoRepository repository,
                            ObjectMapper objectMapper) {
        this.analiseService = analiseService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Analisa o arquivo (OCR + IA) e só então grava. Se o OCR ou a IA falharem, a exceção
     * sobe e nada é salvo. Não é transacional de propósito: a análise leva segundos e não
     * deve segurar uma conexão do banco.
     */
    public DocumentoResponse criar(MultipartFile arquivo, String categoryId) throws IOException {
        String categoria = normalizarCategoria(categoryId); // falha cedo, antes do OCR e da IA
        AnaliseContratoResponse resultado = analiseService.analisarArquivo(arquivo);

        Documento documento = new Documento();
        documento.setNome(nomeSeguro(arquivo.getOriginalFilename()));
        documento.setMimeType(arquivo.getContentType());
        documento.setCategoryId(categoria);
        documento.setProcessingStatus(STATUS_CONCLUIDO);
        documento.setTextoExtraido(resultado.textoExtraido());
        documento.setAnaliseJson(resultado.analise().toString());
        return paraDocumentoResponse(repository.save(documento));
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar() {
        return repository.findAllByOrderByCriadoEmDesc().stream()
                .map(this::paraDocumentoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoResponse buscar(String id) {
        return paraDocumentoResponse(obter(id));
    }

    @Transactional(readOnly = true)
    public AnaliseResponse analise(String id) {
        Documento documento = obter(id);
        JsonNode analise = lerAnalise(documento);
        return new AnaliseResponse(
                documento.getId().toString(),
                documento.getProcessingStatus(),
                AnaliseMapper.resumo(analise),
                AnaliseMapper.pontosChave(analise),
                null);
    }

    /** Lança {@link DocumentoNaoEncontradoException} se o documento não existe. */
    @Transactional(readOnly = true)
    public void verificarExiste(String id) {
        obter(id);
    }

    private Documento obter(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new DocumentoNaoEncontradoException();
        }
        return repository.findById(uuid).orElseThrow(DocumentoNaoEncontradoException::new);
    }

    private DocumentoResponse paraDocumentoResponse(Documento documento) {
        JsonNode analise = lerAnalise(documento);
        return new DocumentoResponse(
                documento.getId().toString(),
                documento.getNome(),
                documento.getCategoryId(),
                documento.getProcessingStatus(),
                documento.getCriadoEm(),
                AnaliseMapper.descricao(analise),
                AnaliseMapper.visualStatus(analise));
    }

    // Um JSON ilegível no banco não deve derrubar a listagem inteira: trata como análise vazia.
    private JsonNode lerAnalise(Documento documento) {
        try {
            return objectMapper.readTree(documento.getAnaliseJson());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Análise ilegível no documento {}", documento.getId(), e);
            return objectMapper.createObjectNode();
        }
    }

    private String normalizarCategoria(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        if (!CATEGORIAS.contains(categoryId)) {
            throw new ContratoInvalidoException("Categoria inválida: " + categoryId + ".");
        }
        return categoryId;
    }

    // O nome vem do cliente: descarta caminhos (C:\pasta\arquivo.pdf, ../arquivo.pdf) e limita o tamanho.
    private String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "Documento";
        }
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).trim();
        if (nome.isEmpty()) {
            return "Documento";
        }
        return nome.length() > TAMANHO_MAX_NOME ? nome.substring(0, TAMANHO_MAX_NOME) : nome;
    }
}
