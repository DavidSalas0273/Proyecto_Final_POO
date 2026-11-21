package com.banco.ledger.fraud.repository;

import com.banco.ledger.fraud.entity.AlertaFraudeJpaEntity;
import com.banco.ledger.fraud.model.RegistroAuditoria;

import org.springframework.stereotype.Repository;

@Repository
public class RepositorioAlertasAdapter implements RepositorioAlertasFraude {
    private final SpringDataAlertaRepository jpa;

    public RepositorioAlertasAdapter(SpringDataAlertaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(RegistroAuditoria registro) {
        jpa.save(AlertaFraudeJpaEntity.desdeAlerta(registro.aAlerta()));
    }
}
