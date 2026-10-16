package com.kaup.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO users (id, email, name, password_hash, enabled, created_at)
            VALUES (:id, :email, :name, :passwordHash, true, :createdAt)
            ON CONFLICT (email) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("email") String email, @Param("name") String name,
                       @Param("passwordHash") String passwordHash, @Param("createdAt") Instant createdAt);
}
