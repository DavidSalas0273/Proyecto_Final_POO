package com.banco.ledger.processor.domain.contract;

import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Comando de transferencia P2P (validación Fail-Fast). */
public record TransferCommandRecord(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("requested_at") Instant requestedAt) implements ComandoLedger {

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

    @Override
    public Transaccion crearTransaccion() {
        return Transaccion.recibirTransferencia(new TransactionId(txId), fromUser, toUser,
                new MontoDinero(amount, currency), requestedAt);
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede ser nulo ni vacío");
        }
        return valor.trim();
    }
}
