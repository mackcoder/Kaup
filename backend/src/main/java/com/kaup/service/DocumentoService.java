package com.kaup.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.kaup.exception.OperacaoNaoPermitidaException;
import com.kaup.exception.FilaCheiaException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.UsuarioLogado;

/** Cria e consulta documentos. Traduz entidade e JSON da IA para o formato do frontend. */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    static final String STATUS_PENDENTE = "pending";
    static final String STATUS_CONCLUIDO = "completed";

    private static final int TAMANHO_MAX_CHAVE = 64;

    private static final int TAMANHO_MAX_NOME = 255;

    private final AnaliseService analiseService;
    private final ProcessadorDeDocumento processador;
    private final Executor executor;
    private final DocumentoRepository repository;
    private final UsuarioRepository usuarios;
    private final ObjectMapper objectMapper;
    private final TetoDiarioGemini teto;

    public DocumentoService(AnaliseService analiseService, ProcessadorDeDocumento processador,
                            @Qualifier("executorAnalise") Executor executor,
                            DocumentoRepository repository, UsuarioRepository usuarios,
                            ObjectMapper objectMapper, TetoDiarioGemini teto) {
        this.analiseService = analiseService;
        this.processador = processador;
        this.executor = executor;
        this.repository = repository;
        this.usuarios = usuarios;
        this.objectMapper = objectMapper;
        this.teto = teto;
    }

    /**
     * Valida o arquivo, grava o documento como "pending" e entrega a leitura (OCR + IA) ao executor
     * de análise; devolve na hora, sem esperar. Os bytes ficam só na memória até a tarefa terminar.
     * Se a fila está cheia, o documento recém-criado é apagado e a pessoa recebe 429. Com uma
     * {@code chave} já usada por ela, devolve o documento que a chave criou em vez de criar outro.
     * Não é transacional de propósito: nada aqui deve segurar uma conexão do banco.
     */
    public DocumentoResponse criar(MultipartFile arquivo, String categoryId, String chave, UsuarioLogado usuario)
            throws IOException {
        String categoria = normalizarCategoria(categoryId); // falha cedo, antes de ler o arquivo
        String chaveLimpa = normalizarChave(chave);
        if (chaveLimpa != null) {
            Optional<Documento> existente = repository.findByDonoIdAndChaveIdempotencia(usuario.id(), chaveLimpa);
            if (existente.isPresent()) {
                return paraDocumentoResponse(existente.get(), usuario);
            }
        }
        analiseService.validar(arquivo);
        teto.verificar(); // com o teto estourado, nem gasta processamento
        PreferenciasDeIA preferencias = preferenciasDe(usuario);
        // Copia agora: o arquivo temporário da requisição pode sumir antes de a tarefa começar.
        String nome = nomeSeguro(arquivo.getOriginalFilename(), "Documento");
        ArquivoEmMemoria copia = new ArquivoEmMemoria(nome, arquivo.getContentType(), arquivo.getBytes());

        Documento documento = new Documento();
        documento.setNome(nome);
        documento.setMimeType(arquivo.getContentType());
        documento.setCategoryId(categoria);
        documento.setProcessingStatus(STATUS_PENDENTE);
        documento.setChaveIdempotencia(chaveLimpa);
        documento.setDono(usuarios.getReferenceById(usuario.id()));
        try {
            documento = repository.save(documento);
        } catch (DataIntegrityViolationException e) {
            // Dois envios com a mesma chave ao mesmo tempo: vale o que chegou primeiro.
            if (chaveLimpa == null) {
                throw e;
            }
            return paraDocumentoResponse(
                    repository.findByDonoIdAndChaveIdempotencia(usuario.id(), chaveLimpa).orElseThrow(() -> e),
                    usuario);
        }

        UUID id = documento.getId();
        try {
            executor.execute(() -> processador.processar(id, copia, categoria, preferencias));
        } catch (RejectedExecutionException e) {
            repository.deleteById(id);
            throw new FilaCheiaException();
        }
        // Recém-salvo, o documento ainda não traz o donoId (coluna só de leitura): o dono é quem enviou.
        return paraDocumentoResponse(documento, true);
    }

    /** Garante que o documento já foi lido; antes disso não há texto nem análise para usar. */
    void exigirConcluido(Documento documento) {
        if (!STATUS_CONCLUIDO.equals(documento.getProcessingStatus())) {
            throw new ContratoInvalidoException(
                    "Este documento ainda não foi lido. Aguarde terminar para continuar.");
        }
    }

    /** O administrador vê os documentos de todos; as demais pessoas, só os próprios. */
    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar(UsuarioLogado usuario) {
        List<Documento> documentos = usuario.isAdmin()
                ? repository.findAllByOrderByCriadoEmDesc()
                : repository.findByDonoIdOrderByCriadoEmDesc(usuario.id());
        return documentos.stream().map(d -> paraDocumentoResponse(d, usuario)).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoResponse buscar(String id, UsuarioLogado usuario) {
        return paraDocumentoResponse(obterParaLeitura(id, usuario), usuario);
    }

    @Transactional(readOnly = true)
    public AnaliseResponse analise(String id, UsuarioLogado usuario) {
        Documento documento = obterParaLeitura(id, usuario);
        JsonNode analise = lerAnalise(documento);
        return new AnaliseResponse(
                documento.getId().toString(),
                documento.getProcessingStatus(),
                AnaliseMapper.resumo(analise),
                AnaliseMapper.pontosChave(analise),
                documento.getFalha());
    }

    /** Apaga o documento; a conversa dele some junto, pela cascata do banco. Só o dono apaga. */
    public void excluir(String id, UsuarioLogado usuario) {
        repository.delete(obterDoDono(id, usuario));
    }

    // Os dois métodos abaixo são visíveis ao pacote: o chat também precisa achar um documento
    // pelo id da URL, com a mesma regra de quem pode ver.

    /** Quem pode ler: o dono e o administrador. Para os demais o documento "não existe" (404). */
    Documento obterParaLeitura(String id, UsuarioLogado usuario) {
        Documento documento = buscarPorId(id);
        if (!usuario.isAdmin() && !ehDono(documento, usuario)) {
            throw new DocumentoNaoEncontradoException();
        }
        return documento;
    }

    /**
     * Quem pode alterar (perguntar no chat, apagar): só o dono. O administrador pode ler o
     * documento de outra pessoa, mas não mexer nele, e recebe um 403 explicando isso.
     */
    Documento obterDoDono(String id, UsuarioLogado usuario) {
        Documento documento = buscarPorId(id);
        if (ehDono(documento, usuario)) {
            return documento;
        }
        if (usuario.isAdmin()) {
            throw new OperacaoNaoPermitidaException(
                    "O administrador só pode consultar os documentos de outras pessoas, não alterá-los.");
        }
        throw new DocumentoNaoEncontradoException();
    }

    private boolean ehDono(Documento documento, UsuarioLogado usuario) {
        return usuario.id().equals(documento.getDonoId());
    }

    private Documento buscarPorId(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new DocumentoNaoEncontradoException();
        }
        return repository.findById(uuid).orElseThrow(DocumentoNaoEncontradoException::new);
    }

    // O perfil é lido na hora do envio: uma análise já feita não muda se o perfil mudar depois.
    private PreferenciasDeIA preferenciasDe(UsuarioLogado usuario) {
        return usuarios.findById(usuario.id()).map(PreferenciasDeIA::de).orElse(PreferenciasDeIA.PADRAO);
    }

    private DocumentoResponse paraDocumentoResponse(Documento documento, UsuarioLogado usuario) {
        return paraDocumentoResponse(documento, ehDono(documento, usuario));
    }

    private DocumentoResponse paraDocumentoResponse(Documento documento, boolean doDono) {
        JsonNode analise = lerAnalise(documento);
        return new DocumentoResponse(
                documento.getId().toString(),
                documento.getNome(),
                documento.getCategoryId(),
                documento.getProcessingStatus(),
                documento.getCriadoEm(),
                AnaliseMapper.descricao(analise),
                STATUS_CONCLUIDO.equals(documento.getProcessingStatus())
                        ? AnaliseMapper.visualStatus(analise) : STATUS_PENDENTE,
                documento.getFalha(),
                doDono);
    }

    // Um JSON ilegível no banco não deve derrubar a listagem inteira: trata como análise vazia.
    private JsonNode lerAnalise(Documento documento) {
        if (documento.getAnaliseJson() == null) { // ainda não lido, ou a leitura falhou
            return objectMapper.createObjectNode();
        }
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
        // Aqui a chave tem que ser exata: um valor errado vindo do frontend é erro de quem chamou.
        if (!Categorias.DESCRICOES.containsKey(categoryId)) {
            throw new ContratoInvalidoException("Categoria inválida: " + categoryId + ".");
        }
        return categoryId;
    }

    private String normalizarChave(String chave) {
        if (chave == null || chave.isBlank()) {
            return null;
        }
        String limpa = chave.trim();
        if (limpa.length() > TAMANHO_MAX_CHAVE) {
            throw new ContratoInvalidoException("Idempotency-Key inválida.");
        }
        return limpa;
    }

    // O nome vem do cliente: descarta caminhos (C:\pasta\arquivo.pdf, ../arquivo.pdf) e limita o tamanho.
    static String nomeSeguro(String nomeOriginal, String padrao) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return padrao;
        }
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).trim();
        if (nome.isEmpty()) {
            return padrao;
        }
        return nome.length() > TAMANHO_MAX_NOME ? nome.substring(0, TAMANHO_MAX_NOME) : nome;
    }
}
