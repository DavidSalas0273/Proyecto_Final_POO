package com.banco.ledger.processor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataEstadoRepository extends JpaRepository<EstadoTransaccionJpaEntity, Long> {

    /** Estado vigente: la fila más reciente del historial para esa transacción. */
    Optional<EstadoTransaccionJpaEntity> findFirstByTxIdOrderByIdDesc(String txId);
}
