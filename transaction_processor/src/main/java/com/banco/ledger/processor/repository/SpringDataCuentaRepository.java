package com.banco.ledger.processor.repository;

import com.banco.ledger.processor.entity.CuentaJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCuentaRepository extends JpaRepository<CuentaJpaEntity, String> {
}
