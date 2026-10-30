package com.kaup.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kaup.entity.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findAllByOrderByCriadoEmDesc();
}
