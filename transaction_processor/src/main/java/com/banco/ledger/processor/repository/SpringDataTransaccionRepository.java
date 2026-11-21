package com.banco.ledger.processor.repository;

import com.banco.ledger.processor.entity.TransaccionJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransaccionRepository extends JpaRepository<TransaccionJpaEntity, String> {
}
