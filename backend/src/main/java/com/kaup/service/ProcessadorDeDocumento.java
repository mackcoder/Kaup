package com.kaup.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.dto.AnaliseContratoResponse;
import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.ServicoExternoException;
import com.kaup.repository.DocumentoRepository;

/**
 * Lê um documento já criado como "pending": OCR, categoria e análise da IA, e então grava o
 * resultado. Roda no executor de análise, nunca na requisição. O motivo de uma falha é uma frase
 * em português para a pessoa; o erro técnico vai só para o log, sem o texto do contrato.
 */
@Service
public class ProcessadorDeDocumento {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorDeDocumento.class);

    static final String STATUS_FALHOU = "failed";
    static final String FALHA_GENERICA = "Não foi possível ler este contrato. Envie de novo.";
    static final String FALHA_IA = "O serviço de leitura está indisponível agora. Envie de novo em instantes.";
    static final String FALHA_INTERROMPIDO = "O envio foi interrompido, envie de novo.";

    private final AnaliseService analiseService;
    private final ServicoDeIA servicoDeIA;
    private final DocumentoRepository repository;

    public ProcessadorDeDocumento(AnaliseService analiseService, ServicoDeIA servicoDeIA,
                                  DocumentoRepository repository) {
        this.analiseService = analiseService;
        this.servicoDeIA = servicoDeIA;
        this.repository = repository;
    }

    /**
     * @param categoria categoria escolhida pela pessoa, ou nulo para a IA classificar
     */
    public void processar(UUID id, MultipartFile arquivo, String categoria, PreferenciasDeIA preferencias) {
        try {
            // Sem linha atualizada, a pessoa apagou o documento enquanto ele esperava na fila.
            if (repository.marcarProcessando(id) == 0) {
                return;
            }
            AnaliseContratoResponse resultado = analiseService.analisarArquivo(arquivo, preferencias);
            String categoriaFinal = categoria != null ? categoria : classificar(resultado.textoExtraido());
            repository.concluir(id, categoriaFinal, resultado.textoExtraido(), resultado.analise().toString());
        } catch (Exception | OutOfMemoryError e) {
            log.warn("A leitura do documento {} falhou", id, e);
            registrarFalha(id, motivo(e));
        }
    }

    // Se nem a gravação da falha der certo (banco fora do ar), a limpeza da próxima subida cobre.
    private void registrarFalha(UUID id, String motivo) {
        try {
            repository.falhar(id, motivo);
        } catch (RuntimeException e) {
            log.error("Não foi possível marcar o documento {} como falho", id, e);
        }
    }

    private String motivo(Throwable erro) {
        if (erro instanceof ContratoInvalidoException) {
            return erro.getMessage(); // já é uma frase para a pessoa ("PDF protegido por senha"...)
        }
        if (erro instanceof ServicoExternoException) {
            return FALHA_IA;
        }
        return FALHA_GENERICA;
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
}
