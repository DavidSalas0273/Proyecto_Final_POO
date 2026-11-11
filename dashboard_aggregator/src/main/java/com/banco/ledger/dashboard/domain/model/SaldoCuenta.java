package com.banco.ledger.dashboard.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Saldo proyectado de una cuenta según el último evento del ledger que la afectó. */
public record SaldoCuenta(
        @JsonProperty("user") String usuario,
        @JsonProperty("balance") BigDecimal saldo,
        @JsonProperty("currency") String divisa,
        @JsonProperty("updated_at") Instant actualizadoEn) {

    public SaldoCuenta {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (saldo == null) {
            throw new IllegalArgumentException("El saldo es obligatorio");
        }
    }

    /** Conserva el saldo más reciente cuando los eventos llegan desordenados (particiones distintas). */
    public SaldoCuenta masRecienteEntre(SaldoCuenta otro) {
        boolean otroEsMasNuevo = actualizadoEn == null
                || (otro.actualizadoEn != null && !otro.actualizadoEn.isBefore(actualizadoEn));
        return otroEsMasNuevo ? otro : this;
    }
}
