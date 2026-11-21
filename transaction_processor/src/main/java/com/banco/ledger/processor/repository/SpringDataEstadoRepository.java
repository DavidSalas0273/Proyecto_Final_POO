package com.banco.ledger.processor.repository;

import com.banco.ledger.processor.entity.EstadoTransaccionJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface SpringDataEstadoRepository extends JpaRepository<EstadoTransaccionJpaEntity, Long> {
    Optional<EstadoTransaccionJpaEntity> findFirstByTxIdOrderByIdDesc(String txId);
}
