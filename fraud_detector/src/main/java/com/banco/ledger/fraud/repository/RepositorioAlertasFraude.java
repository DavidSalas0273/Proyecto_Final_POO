package com.banco.ledger.fraud.repository;

import com.banco.ledger.fraud.model.RegistroAuditoria;

public interface RepositorioAlertasFraude {
    void registrar(RegistroAuditoria registro);
}
