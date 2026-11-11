package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Rechaza transferencias que superan el límite operativo por transacción. */
public class LimitePorTransaccionRegla implements ReglaValidacionTransaccion {

    public static final String MOTIVO = "AMOUNT_LIMIT_EXCEEDED";

    private final BigDecimal limite;

    public LimitePorTransaccionRegla(BigDecimal limite) {
        this.limite = Objects.requireNonNull(limite, "El límite es obligatorio");
        if (limite.signum() <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor que cero");
        }
    }

    @Override
    public Optional<String> evaluar(Transaccion transaccion, CuentaBancaria origen, CuentaBancaria destino) {
        return Optional.of(MOTIVO)
                .filter(motivo -> transaccion.monto().valor().compareTo(limite) > 0);
    }
}
