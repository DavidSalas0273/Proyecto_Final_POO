package com.banco.ledger.processor.regla;

import com.banco.ledger.processor.model.CuentaBancaria;
import com.banco.ledger.processor.model.Transaccion;

import java.util.Optional;

public class DivisaCompatibleRegla implements ReglaValidacionTransaccion {
    public static final String MOTIVO = "CURRENCY_MISMATCH";

    @Override
    public Optional<String> evaluar(Transaccion transaccion, Optional<CuentaBancaria> origen,
                                    CuentaBancaria destino) {
        boolean compatible = destino.saldo().mismaDivisaQue(transaccion.monto())
                && origen.map(cuenta -> cuenta.saldo().mismaDivisaQue(transaccion.monto())).orElse(true);
        return Optional.of(MOTIVO).filter(motivo -> !compatible);
    }
}
