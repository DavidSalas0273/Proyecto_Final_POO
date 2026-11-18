package com.banco.ledger.api.domain.usuario;

import java.time.Instant;

public record Sesion(String token, String usuario, String nombreCompleto, Instant expiraEn) {
    public Sesion {
        if (token == null || token.length() < 32) {
            throw new IllegalArgumentException("El token de sesión no es válido");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("La sesión debe tener un titular");
        }
        if (expiraEn == null) {
            throw new IllegalArgumentException("La sesión debe tener vencimiento");
        }
    }

    public boolean estaVigente(Instant ahora) {
        return expiraEn.isAfter(ahora);
    }

    @Override
    public String toString() {
        return "Sesion[usuario=" + usuario + ", expiraEn=" + expiraEn + "]";
    }
}
