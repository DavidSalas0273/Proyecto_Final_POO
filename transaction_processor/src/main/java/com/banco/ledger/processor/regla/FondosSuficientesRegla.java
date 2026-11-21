package com.banco.ledger.processor.regla;

import com.banco.ledger.processor.model.CuentaBancaria;
import com.banco.ledger.processor.model.Transaccion;

import java.util.Optional;

public class FondosSuficientesRegla implements ReglaValidacionTransaccion {
    @Override
    public Optional<String> evaluar(Transaccion transaccion, Optional<CuentaBancaria> origen,
                                    CuentaBancaria destino) {
        return origen.filter(cuenta -> !cuenta.tieneFondosPara(transaccion.monto()))
                .map(cuenta -> Transaccion.MOTIVO_FONDOS_INSUFICIENTES);
    }
}
