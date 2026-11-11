package com.banco.ledger.api.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

/**
 * Comando inmutable "el usuario X recarga su saldo con Y".
 * Viaja por la misma Work Queue que las transferencias para conservar el orden.
 */
public record DepositCommandRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("requested_at") Instant requestedAt) implements ComandoLedger {

    public DepositCommandRecord {
        txId = requerirTexto(txId, "tx_id");
        toUser = requerirTexto(toUser, "user");
        currency = requerirTexto(currency, "currency").toUpperCase(Locale.ROOT);

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
        if (!TransferCommandRecord.DIVISAS_ADMITIDAS.contains(currency)) {
            throw new IllegalArgumentException("Divisa no admitida: " + currency);
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
