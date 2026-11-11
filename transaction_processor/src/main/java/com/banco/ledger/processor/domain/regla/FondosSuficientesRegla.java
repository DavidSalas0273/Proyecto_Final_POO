package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.Optional;

/** Rechaza la transferencia si la cuenta de origen no tiene saldo suficiente. */
public class FondosSuficientesRegla implements ReglaValidacionTransaccion {

    @Override
    public Optional<String> evaluar(Transaccion transaccion, CuentaBancaria origen, CuentaBancaria destino) {
        return Optional.of(Transaccion.MOTIVO_FONDOS_INSUFICIENTES)
                .filter(motivo -> !origen.tieneFondosPara(transaccion.monto()));
    }
}
