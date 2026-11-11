package com.banco.ledger.processor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransaccionRepository extends JpaRepository<TransaccionJpaEntity, String> {
}
