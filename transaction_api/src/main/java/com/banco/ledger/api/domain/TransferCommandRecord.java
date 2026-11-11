package com.banco.ledger.api.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Comando inmutable "Usuario A quiere enviar X a Usuario B".
 * Es el contrato que viaja por la Work Queue de RabbitMQ (transfer_commands).
 * El constructor compacto aplica validaciones Fail-Fast.
 */
public record TransferCommandRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("requested_at") Instant requestedAt) {

    public static final Set<String> DIVISAS_ADMITIDAS = Set.of("USD", "EUR", "COP");

    public TransferCommandRecord {
        txId = requerirTexto(txId, "tx_id");
        fromUser = requerirTexto(fromUser, "from_user");
        toUser = requerirTexto(toUser, "to_user");
        currency = requerirTexto(currency, "currency").toUpperCase(Locale.ROOT);

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
        if (!DIVISAS_ADMITIDAS.contains(currency)) {
            throw new IllegalArgumentException("Divisa no admitida: " + currency);
        }
        if (fromUser.equalsIgnoreCase(toUser)) {
            throw new IllegalArgumentException("from_user y to_user no pueden ser el mismo usuario");
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
