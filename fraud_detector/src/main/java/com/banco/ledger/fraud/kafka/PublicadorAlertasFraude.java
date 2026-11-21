package com.banco.ledger.fraud.kafka;

import com.banco.ledger.fraud.dto.FraudAlertRecord;

public interface PublicadorAlertasFraude {
    void publicar(FraudAlertRecord alerta);
}
