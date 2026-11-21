package com.banco.ledger.api.repository;

import com.banco.ledger.api.entity.SesionJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSesionRepository extends JpaRepository<SesionJpaEntity, String> {
}
