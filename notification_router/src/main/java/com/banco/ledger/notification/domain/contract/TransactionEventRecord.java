package com.banco.ledger.notification.domain.contract;

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

    public static final String TIPO_TRANSFERENCIA = "TRANSFER";

    public TransactionEventRecord {
        type = (type == null || type.isBlank()) ? TIPO_TRANSFERENCIA : type;
        requerirTexto(txId, "tx_id");
        requerirTexto(toUser, "to_user");
        requerirTexto(currency, "currency");
        requerirTexto(status, "status");
        if (TIPO_TRANSFERENCIA.equals(type)) {
            requerirTexto(fromUser, "from_user");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
    }

    /** Clave "TIPO:ESTADO" usada para elegir la plantilla de notificación. */
    public String clave() {
        return type + ":" + status;
    }

    private static void requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede ser nulo ni vacío");
        }
    }
}
