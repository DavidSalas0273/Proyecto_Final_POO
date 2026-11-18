package com.banco.ledger.api.domain.usuario;

import java.time.Instant;

public class CuentaBloqueadaException extends RuntimeException {
    private final transient Instant hasta;

    public CuentaBloqueadaException(Instant hasta) {
        super("Bloqueamos el acceso por seguridad tras " + Usuario.MAX_INTENTOS + " intentos fallidos");
        this.hasta = hasta;
    }

    public Instant hasta() {
        return hasta;
    }
}
