package com.kaup.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kaup.entity.MensagemChat;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    /** Quantas mensagens de {@code papel} foram criadas a partir de {@code instante}. */
    long countByPapelAndCriadoEmGreaterThanEqual(String papel, Instant instante);

    /** Conversa completa, da mais antiga para a mais recente. */
    List<MensagemChat> findByDocumentoIdOrderByIdAsc(UUID documentoId);

    /** As mais recentes primeiro; o tamanho vem do {@code Pageable}. */
    List<MensagemChat> findByDocumentoIdOrderByIdDesc(UUID documentoId, Pageable limite);
}
