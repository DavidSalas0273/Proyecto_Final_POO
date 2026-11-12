package com.banco.ledger.processor.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Alerta consumida desde el topic fraud_alerts (solo los campos que usa el procesador). */
public record FraudAlertRecord(@JsonProperty("tx_id") String txId,
                               @JsonProperty("reason") String reason) {

    public FraudAlertRecord {
        if (txId == null || txId.isBlank()) {
            throw new IllegalArgumentException("tx_id no puede ser nulo ni vacío");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason no puede ser nulo ni vacío");
        }
    }
}
