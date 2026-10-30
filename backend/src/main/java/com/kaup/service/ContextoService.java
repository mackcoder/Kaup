package com.kaup.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnexoResponse;
import com.kaup.entity.AnexoContexto;
import com.kaup.entity.Documento;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.repository.AnexoContextoRepository;
import com.kaup.security.UsuarioLogado;

/** Arquivos extras anexados à conversa de um documento. Só o dono anexa; quem pode ler, lista. */
@Service
public class ContextoService {

    /** Quantos anexos cada documento aceita. */
    static final int ANEXOS_MAX = 5;

    /** Limite de texto de um anexo (caracteres); o excedente é cortado para não estourar o prompt. */
    static final int TEXTO_MAX_POR_ANEXO = 30_000;

    private static final int TAMANHO_MAX_NOME = 255;

    private final DocumentoService documentoService;
    private final AnaliseService analiseService;
    private final AnexoContextoRepository repository;

    public ContextoService(DocumentoService documentoService, AnaliseService analiseService,
                           AnexoContextoRepository repository) {
        this.documentoService = documentoService;
        this.analiseService = analiseService;
        this.repository = repository;
    }

    /**
     * Lê o arquivo por OCR e guarda o texto. Não é transacional de propósito: o OCR leva segundos
     * e não deve segurar uma conexão do banco.
     */
    public AnexoResponse anexar(String documentoId, MultipartFile arquivo, UsuarioLogado usuario)
            throws IOException {
        Documento documento = documentoService.obterDoDono(documentoId, usuario);
        if (repository.countByDocumentoId(documento.getId()) >= ANEXOS_MAX) {
            throw new ContratoInvalidoException(
                    "Cada documento aceita até " + ANEXOS_MAX + " anexos. Exclua o documento ou envie um novo.");
        }
        String texto = analiseService.extrairTexto(arquivo);
        if (texto.length() > TEXTO_MAX_POR_ANEXO) {
            texto = texto.substring(0, TEXTO_MAX_POR_ANEXO);
        }

        AnexoContexto anexo = new AnexoContexto();
        anexo.setDocumento(documento);
        anexo.setNome(DocumentoService.nomeSeguro(arquivo.getOriginalFilename(), "Anexo"));
        anexo.setMimeType(arquivo.getContentType());
        anexo.setTextoExtraido(texto);
        return paraResponse(repository.save(anexo), documento);
    }

    @Transactional(readOnly = true)
    public List<AnexoResponse> listar(String documentoId, UsuarioLogado usuario) {
        Documento documento = documentoService.obterParaLeitura(documentoId, usuario);
        return repository.findByDocumentoIdOrderByIdAsc(documento.getId()).stream()
                .map(a -> paraResponse(a, documento))
                .toList();
    }

    private AnexoResponse paraResponse(AnexoContexto anexo, Documento documento) {
        return new AnexoResponse(
                anexo.getId().toString(),
                documento.getId().toString(),
                anexo.getNome(),
                anexo.getMimeType(),
                anexo.getCriadoEm());
    }
}
