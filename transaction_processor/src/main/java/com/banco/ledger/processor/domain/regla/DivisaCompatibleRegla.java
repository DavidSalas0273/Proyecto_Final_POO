package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.Optional;

/** Rechaza transferencias cuya divisa no coincide con la de las cuentas involucradas. */
public class DivisaCompatibleRegla implements ReglaValidacionTransaccion {

    public static final String MOTIVO = "CURRENCY_MISMATCH";

    @Override
    public Optional<String> evaluar(Transaccion transaccion, CuentaBancaria origen, CuentaBancaria destino) {
        boolean compatible = origen.saldo().mismaDivisaQue(transaccion.monto())
                && destino.saldo().mismaDivisaQue(transaccion.monto());
        return Optional.of(MOTIVO).filter(motivo -> !compatible);
    }
}
