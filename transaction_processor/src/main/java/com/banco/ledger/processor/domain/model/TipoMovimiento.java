package com.banco.ledger.processor.domain.model;

import java.util.Optional;

public enum TipoMovimiento {
    TRANSFER {
        @Override
        void aplicar(MontoDinero monto, Optional<CuentaBancaria> origen, CuentaBancaria destino) {
            origen.orElseThrow(() -> new IllegalStateException("Una transferencia requiere cuenta de origen"))
                    .debitar(monto);
            destino.acreditar(monto);
        }
    },

    DEPOSIT {
        @Override
        void aplicar(MontoDinero monto, Optional<CuentaBancaria> origen, CuentaBancaria destino) {
            destino.acreditar(monto);
        }
    };

    abstract void aplicar(MontoDinero monto, Optional<CuentaBancaria> origen, CuentaBancaria destino);
}
