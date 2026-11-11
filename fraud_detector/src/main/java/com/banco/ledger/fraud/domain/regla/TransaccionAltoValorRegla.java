package com.banco.ledger.fraud.domain.regla;

import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.banco.ledger.fraud.domain.model.HistorialTransacciones;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Alerta cuando el monto supera el umbral configurado (por defecto $10,000). */
public class TransaccionAltoValorRegla implements ReglaDeteccionFraude {

    public static final String MOTIVO = "HIGH_VALUE_TRANSACTION";

    private final BigDecimal umbral;

    public TransaccionAltoValorRegla(BigDecimal umbral) {
        this.umbral = Objects.requireNonNull(umbral, "El umbral es obligatorio");
        if (umbral.signum() <= 0) {
            throw new IllegalArgumentException("El umbral debe ser mayor que cero");
        }
    }

    @Override
    public Optional<String> evaluar(TransactionEventRecord evento, HistorialTransacciones historial) {
        return Optional.of(MOTIVO).filter(motivo -> evento.amount().compareTo(umbral) > 0);
    }
}
