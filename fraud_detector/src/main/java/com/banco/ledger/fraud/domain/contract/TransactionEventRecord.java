package com.banco.ledger.fraud.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento del ledger consumido desde transactions_log. */
public record TransactionEventRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") String status,
        @JsonProperty("reason") String reason,
        @JsonProperty("timestamp") Instant timestamp) {

    public static final String ESTADO_COMPLETADO = "COMPLETED";

    public TransactionEventRecord {
        requerirTexto(txId, "tx_id");
        requerirTexto(fromUser, "from_user");
        requerirTexto(toUser, "to_user");
        requerirTexto(status, "status");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser nulo");
        }
    }

    public boolean estaCompletada() {
        return ESTADO_COMPLETADO.equals(status);
    }

    private static void requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede ser nulo ni vacío");
        }
    }
}
