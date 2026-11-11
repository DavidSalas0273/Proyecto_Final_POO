package com.banco.ledger.fraud.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Alerta de fraude inmutable publicada en el topic fraud_alerts (key = tx_id). */
public record FraudAlertRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("reason") String reason,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("detected_at") Instant detectedAt) {

    public FraudAlertRecord {
        if (txId == null || txId.isBlank()) {
            throw new IllegalArgumentException("tx_id no puede ser nulo ni vacío");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason no puede ser nulo ni vacío");
        }
        if (detectedAt == null) {
            throw new IllegalArgumentException("detected_at no puede ser nulo");
        }
    }

    public static FraudAlertRecord de(TransactionEventRecord evento, String motivo, Instant detectadaEn) {
        return new FraudAlertRecord(evento.txId(), motivo, evento.fromUser(), evento.amount(), detectadaEn);
    }
}
