package com.banco.ledger.processor.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento inmutable publicado en el ledger de Kafka (topic transactions_log, key = tx_id).
 * <ul>
 *   <li>type: TRANSFER (transferencia P2P) o DEPOSIT (recarga; sin from_user).</li>
 *   <li>reason: solo en movimientos rechazados.</li>
 *   <li>from_balance / to_balance: saldos resultantes de las cuentas tras procesar el movimiento.</li>
 * </ul>
 */
public record TransactionEventRecord(
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
        @JsonProperty("timestamp") Instant timestamp) {

    public static final String TIPO_TRANSFERENCIA = "TRANSFER";

    public TransactionEventRecord {
        requerirTexto(txId, "tx_id");
        requerirTexto(type, "type");
        requerirTexto(toUser, "to_user");
        requerirTexto(currency, "currency");
        requerirTexto(status, "status");
        if (TIPO_TRANSFERENCIA.equals(type)) {
            requerirTexto(fromUser, "from_user");
        }
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
