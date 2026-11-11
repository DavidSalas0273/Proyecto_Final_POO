package com.banco.ledger.fraud.application.port;

import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;

/** Puerto de persistencia para la auditoría forense de alertas. */
public interface RepositorioAlertasFraude {

    void registrar(FraudAlertRecord alerta);
}
