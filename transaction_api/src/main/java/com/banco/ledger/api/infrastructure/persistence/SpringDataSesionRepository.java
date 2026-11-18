package com.banco.ledger.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSesionRepository extends JpaRepository<SesionJpaEntity, String> {
}
