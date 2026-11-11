package com.banco.ledger.processor.domain.model;

import java.util.Optional;

/**
 * Tipo de movimiento contable. Cada constante define polimórficamente cómo se
 * aplica el movimiento sobre las cuentas (Strategy embebido en el enum).
 */
public enum TipoMovimiento {

    /** Transferencia P2P: debita el origen y acredita el destino. */
    TRANSFER {
        @Override
        void aplicar(MontoDinero monto, Optional<CuentaBancaria> origen, CuentaBancaria destino) {
            origen.orElseThrow(() -> new IllegalStateException("Una transferencia requiere cuenta de origen"))
                    .debitar(monto);
            destino.acreditar(monto);
        }
    },

    /** Recarga de saldo: solo acredita la cuenta del titular. */
    DEPOSIT {
        @Override
        void aplicar(MontoDinero monto, Optional<CuentaBancaria> origen, CuentaBancaria destino) {
            destino.acreditar(monto);
        }
    };

    abstract void aplicar(MontoDinero monto, Optional<CuentaBancaria> origen, CuentaBancaria destino);
}
