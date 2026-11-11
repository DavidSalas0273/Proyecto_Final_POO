package com.banco.ledger.dashboard.domain.model;

import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Fila inmutable de la vista materializada; los cambios producen nuevas instancias. */
public record TransaccionVista(
        @JsonProperty("tx_id") String txId,
        @JsonProperty("type") String type,
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") String status,
        @JsonProperty("reason") String reason,
        @JsonProperty("from_balance") BigDecimal fromBalance,
        @JsonProperty("to_balance") BigDecimal toBalance,
        @JsonProperty("suspicious") boolean suspicious,
        @JsonProperty("fraud_reason") String fraudReason,
        @JsonProperty("timestamp") Instant timestamp) {

    public static TransaccionVista desde(TransactionEventRecord evento) {
        return new TransaccionVista(evento.txId(), evento.type(), evento.fromUser(), evento.toUser(),
                evento.amount(), evento.currency(), evento.status(), evento.reason(), evento.fromBalance(),
                evento.toBalance(), false, null, evento.timestamp());
    }

    public TransaccionVista marcarSospechosa(String motivo) {
        return new TransaccionVista(txId, type, fromUser, toUser, amount, currency, status, reason, fromBalance,
                toBalance, true, motivo, timestamp);
    }
}
