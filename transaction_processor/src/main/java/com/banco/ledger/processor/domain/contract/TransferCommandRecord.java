package com.banco.ledger.processor.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Comando consumido desde la Work Queue transfer_commands (validación Fail-Fast). */
public record TransferCommandRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("requested_at") Instant requestedAt) {

    public TransferCommandRecord {
        txId = requerirTexto(txId, "tx_id");
        fromUser = requerirTexto(fromUser, "from_user");
        toUser = requerirTexto(toUser, "to_user");
        currency = requerirTexto(currency, "currency");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requested_at no puede ser nulo");
        }
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede ser nulo ni vacío");
        }
        return valor.trim();
    }
}
