package com.banco.ledger.dashboard.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento del ledger consumido desde transactions_log.
 * type: TRANSFER o DEPOSIT (si falta se asume TRANSFER). from_balance / to_balance: saldos resultantes.
 */
public record TransactionEventRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") String status,
        @JsonProperty("reason") String reason,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("type") String type,
        @JsonProperty("from_balance") BigDecimal fromBalance,
        @JsonProperty("to_balance") BigDecimal toBalance) {

    public TransactionEventRecord {
        type = (type == null || type.isBlank()) ? "TRANSFER" : type;
        if (txId == null || txId.isBlank()) {
            throw new IllegalArgumentException("tx_id no puede ser nulo ni vacío");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status no puede ser nulo ni vacío");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
    }
}
