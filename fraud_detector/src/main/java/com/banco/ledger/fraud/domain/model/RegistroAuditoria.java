package com.banco.ledger.fraud.domain.model;

import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;
import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class RegistroAuditoria {
    private final String txId;
    private final String usuario;
    private final BigDecimal monto;
    private final String motivo;
    private final Instant detectadaEn;

    private RegistroAuditoria(String txId, String usuario, BigDecimal monto, String motivo, Instant detectadaEn) {
        this.txId = txId;
        this.usuario = usuario;
        this.monto = monto;
        this.motivo = motivo;
        this.detectadaEn = detectadaEn;
    }

    public static RegistroAuditoria abrirPorSospecha(TransactionEventRecord evento, String motivo,
                                                     Instant detectadaEn) {
        Objects.requireNonNull(evento, "El evento es obligatorio");
        if (!evento.esTransferenciaCompletada()) {
            throw new IllegalStateException(
                    "Solo se auditan transferencias completadas; la transacción " + evento.txId()
                            + " está en estado " + evento.status());
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de la sospecha es obligatorio");
        }
        if (detectadaEn == null) {
            throw new IllegalArgumentException("La fecha de detección es obligatoria");
        }
        return new RegistroAuditoria(evento.txId(), evento.fromUser(), evento.amount(), motivo.trim(), detectadaEn);
    }

    public FraudAlertRecord aAlerta() {
        return new FraudAlertRecord(txId, motivo, usuario, monto, detectadaEn);
    }

    public String txId() {
        return txId;
    }

    public String motivo() {
        return motivo;
    }
}
