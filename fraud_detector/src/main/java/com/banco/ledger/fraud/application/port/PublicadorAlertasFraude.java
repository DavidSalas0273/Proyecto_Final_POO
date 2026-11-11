package com.banco.ledger.fraud.application.port;

import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;

/** Puerto de mensajería para publicar alertas en fraud_alerts. */
public interface PublicadorAlertasFraude {

    void publicar(FraudAlertRecord alerta);
}
