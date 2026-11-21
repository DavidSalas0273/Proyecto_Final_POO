package com.banco.ledger.fraud.regla;

import com.banco.ledger.fraud.dto.TransactionEventRecord;
import com.banco.ledger.fraud.model.HistorialTransacciones;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

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
