package com.banco.ledger.fraud.infrastructure.persistence;

import com.banco.ledger.fraud.application.port.RepositorioAlertasFraude;
import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;
import org.springframework.stereotype.Repository;

/** Adaptador de persistencia de alertas (dominio -> tabla alertas_fraude). */
@Repository
public class RepositorioAlertasAdapter implements RepositorioAlertasFraude {

    private final SpringDataAlertaRepository jpa;

    public RepositorioAlertasAdapter(SpringDataAlertaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(FraudAlertRecord alerta) {
        jpa.save(AlertaFraudeJpaEntity.desdeAlerta(alerta));
    }
}
