package com.banco.ledger.api.domain.usuario;

import java.time.Duration;

public record PoliticaSesion(Duration duracion) {
    public PoliticaSesion {
        if (duracion == null || duracion.isNegative() || duracion.isZero()) {
            throw new IllegalArgumentException("La duración de la sesión debe ser positiva");
        }
    }
}
