package com.kaup.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kaup.entity.AnexoContexto;

public interface AnexoContextoRepository extends JpaRepository<AnexoContexto, Long> {

    /** Do mais antigo para o mais recente (a ordem em que a IA os recebe). */
    List<AnexoContexto> findByDocumentoIdOrderByIdAsc(UUID documentoId);

    long countByDocumentoId(UUID documentoId);
}
