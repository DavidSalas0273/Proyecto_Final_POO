package com.banco.ledger.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataUsuarioRepository extends JpaRepository<UsuarioJpaEntity, String> {
    boolean existsByUsuarioIgnoreCase(String usuario);

    boolean existsByEmail(String email);

    boolean existsByDocumento(String documento);

    Optional<UsuarioJpaEntity> findFirstByUsuarioIgnoreCaseOrEmail(String usuario, String email);
}
