package com.banco.ledger.processor.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento inmutable publicado en el ledger de Kafka (topic transactions_log, key = tx_id).
 * reason es opcional (solo en transacciones rechazadas o sospechosas).
 */
public record TransactionEventRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") String status,
        @JsonProperty("reason") String reason,
        @JsonProperty("timestamp") Instant timestamp) {

    public TransactionEventRecord {
        requerirTexto(txId, "tx_id");
        requerirTexto(fromUser, "from_user");
        requerirTexto(toUser, "to_user");
        requerirTexto(currency, "currency");
        requerirTexto(status, "status");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser nulo");
        }
    }

    private static void requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede ser nulo ni vacío");
        }
    }
}
