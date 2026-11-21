package com.banco.ledger.fraud.repository;

import com.banco.ledger.fraud.entity.AlertaFraudeJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAlertaRepository extends JpaRepository<AlertaFraudeJpaEntity, Long> {
}
