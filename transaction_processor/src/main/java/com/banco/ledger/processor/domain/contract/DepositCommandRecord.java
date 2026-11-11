package com.banco.ledger.processor.domain.contract;

import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Comando de recarga de saldo para un usuario (validación Fail-Fast). */
public record DepositCommandRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("requested_at") Instant requestedAt) implements ComandoLedger {

    public DepositCommandRecord {
        txId = requerirTexto(txId, "tx_id");
        toUser = requerirTexto(toUser, "to_user");
        currency = requerirTexto(currency, "currency");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requested_at no puede ser nulo");
        }
    }

    @Override
    public Transaccion crearTransaccion() {
        return Transaccion.recibirRecarga(new TransactionId(txId), toUser, new MontoDinero(amount, currency),
                requestedAt);
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede ser nulo ni vacío");
        }
        return valor.trim();
    }
}
