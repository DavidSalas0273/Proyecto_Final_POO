package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.Optional;

/**
 * Deniega el movimiento si la cuenta de origen no tiene saldo suficiente.
 * Las recargas no tienen origen, por lo que nunca se deniegan por esta regla.
 */
public class FondosSuficientesRegla implements ReglaValidacionTransaccion {

    @Override
    public Optional<String> evaluar(Transaccion transaccion, Optional<CuentaBancaria> origen,
                                    CuentaBancaria destino) {
        return origen.filter(cuenta -> !cuenta.tieneFondosPara(transaccion.monto()))
                .map(cuenta -> Transaccion.MOTIVO_FONDOS_INSUFICIENTES);
    }
}
