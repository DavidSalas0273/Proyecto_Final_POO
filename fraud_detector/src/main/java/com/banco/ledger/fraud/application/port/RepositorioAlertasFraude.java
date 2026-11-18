package com.banco.ledger.fraud.application.port;

import com.banco.ledger.fraud.domain.model.RegistroAuditoria;

public interface RepositorioAlertasFraude {
    void registrar(RegistroAuditoria registro);
}
