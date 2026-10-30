package com.kaup.service;

import java.io.IOException;
import java.util.List;
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
import com.kaup.exception.OperacaoNaoPermitidaException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.DocumentoRepository;
import com.kaup.repository.UsuarioRepository;
import com.kaup.security.UsuarioLogado;

/** Cria e consulta documentos. Traduz entidade e JSON da IA para o formato do frontend. */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    static final String STATUS_CONCLUIDO = "completed";

    private static final int TAMANHO_MAX_NOME = 255;

    private final AnaliseService analiseService;
    private final ServicoDeIA servicoDeIA;
    private final DocumentoRepository repository;
    private final UsuarioRepository usuarios;
    private final ObjectMapper objectMapper;

    public DocumentoService(AnaliseService analiseService, ServicoDeIA servicoDeIA,
                            DocumentoRepository repository, UsuarioRepository usuarios,
                            ObjectMapper objectMapper) {
        this.analiseService = analiseService;
        this.servicoDeIA = servicoDeIA;
        this.repository = repository;
        this.usuarios = usuarios;
        this.objectMapper = objectMapper;
    }

    /**
     * Analisa o arquivo (OCR + IA) e só então grava. Se o OCR ou a análise falharem, a exceção
     * sobe e nada é salvo. Se o usuário não escolheu categoria, a IA classifica. Não é
     * transacional de propósito: a análise leva segundos e não deve segurar uma conexão do banco.
     */
    public DocumentoResponse criar(MultipartFile arquivo, String categoryId, UsuarioLogado usuario)
            throws IOException {
        String categoria = normalizarCategoria(categoryId); // falha cedo, antes do OCR e da IA
        PreferenciasDeIA preferencias = preferenciasDe(usuario);
        AnaliseContratoResponse resultado = analiseService.analisarArquivo(arquivo, preferencias);
        if (categoria == null) {
            categoria = classificar(resultado.textoExtraido());
        }

        Documento documento = new Documento();
        documento.setNome(nomeSeguro(arquivo.getOriginalFilename()));
        documento.setMimeType(arquivo.getContentType());
        documento.setCategoryId(categoria);
        documento.setProcessingStatus(STATUS_CONCLUIDO);
        documento.setTextoExtraido(resultado.textoExtraido());
        documento.setAnaliseJson(resultado.analise().toString());
        documento.setDono(usuarios.getReferenceById(usuario.id()));
        return paraDocumentoResponse(repository.save(documento), usuario);
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
                null);
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
        JsonNode analise = lerAnalise(documento);
        return new DocumentoResponse(
                documento.getId().toString(),
                documento.getNome(),
                documento.getCategoryId(),
                documento.getProcessingStatus(),
                documento.getCriadoEm(),
                AnaliseMapper.descricao(analise),
                AnaliseMapper.visualStatus(analise),
                ehDono(documento, usuario));
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
        // Aqui a chave tem que ser exata: um valor errado vindo do frontend é erro de quem chamou.
        if (!Categorias.DESCRICOES.containsKey(categoryId)) {
            throw new ContratoInvalidoException("Categoria inválida: " + categoryId + ".");
        }
        return categoryId;
    }

    // A classificação é um extra: se a IA falhar ou responder algo fora da lista, o documento
    // continua válido e vai para "Outros", em vez de perder um upload que já foi analisado.
    private String classificar(String texto) {
        try {
            String categoria = Categorias.normalizar(servicoDeIA.classificar(texto, Categorias.DESCRICOES));
            return categoria != null ? categoria : Categorias.OUTROS;
        } catch (ServicoExternoException e) {
            log.warn("Não foi possível classificar o documento; usando '{}'", Categorias.OUTROS, e);
            return Categorias.OUTROS;
        }
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
