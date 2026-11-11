package com.banco.ledger.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAlertaRepository extends JpaRepository<AlertaFraudeJpaEntity, Long> {
}
