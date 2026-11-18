package com.banco.ledger.api.domain.usuario;

import java.time.Duration;

/** Cuánto dura una sesión de la banca en línea antes de pedir de nuevo la contraseña. */
public record PoliticaSesion(Duration duracion) {

    public PoliticaSesion {
        if (duracion == null || duracion.isNegative() || duracion.isZero()) {
            throw new IllegalArgumentException("La duración de la sesión debe ser positiva");
        }
    }
}
