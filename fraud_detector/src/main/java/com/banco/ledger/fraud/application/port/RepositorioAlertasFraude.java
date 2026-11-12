package com.banco.ledger.fraud.application.port;

import com.banco.ledger.fraud.domain.model.RegistroAuditoria;

/** Puerto de persistencia para la auditoría forense de transacciones sospechosas. */
public interface RepositorioAlertasFraude {

    void registrar(RegistroAuditoria registro);
}
