package com.banco.ledger.processor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataEstadoRepository extends JpaRepository<EstadoTransaccionJpaEntity, Long> {
    Optional<EstadoTransaccionJpaEntity> findFirstByTxIdOrderByIdDesc(String txId);
}
