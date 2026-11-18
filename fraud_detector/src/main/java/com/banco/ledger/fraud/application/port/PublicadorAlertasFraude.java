package com.banco.ledger.fraud.application.port;

import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;

public interface PublicadorAlertasFraude {
    void publicar(FraudAlertRecord alerta);
}
