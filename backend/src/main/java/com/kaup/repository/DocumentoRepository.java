package com.kaup.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kaup.entity.Documento;
import com.kaup.entity.Usuario;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findAllByOrderByCriadoEmDesc();

    /** Documentos de uma pessoa, do mais recente para o mais antigo. */
    List<Documento> findByDonoIdOrderByCriadoEmDesc(UUID donoId);

    /** Entrega ao usuário os documentos que ainda não têm dono. Devolve quantos foram. */
    @Modifying
    @Query("update Documento d set d.dono = :dono where d.dono is null")
    int atribuirSemDono(@Param("dono") Usuario dono);

    /**
     * Apaga os documentos de usuários comuns criados antes de {@code limite} (as conversas vão
     * junto, pela cascata do banco). Ignora os sem dono e os do administrador. Devolve quantos foram.
     */
    @Modifying
    @Query("""
            delete from Documento d
            where d.criadoEm < :limite
              and d.dono.id in (select u.id from Usuario u where u.papel = com.kaup.entity.Papel.USER)""")
    int apagarDeUsuariosAnterioresA(@Param("limite") Instant limite);
}
