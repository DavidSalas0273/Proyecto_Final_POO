package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.Optional;

/**
 * Patrón Strategy: cada regla de riesgo/validación es intercambiable.
 * Devuelve el motivo de rechazo o vacío si la transacción cumple la regla.
 */
@FunctionalInterface
public interface ReglaValidacionTransaccion {

    Optional<String> evaluar(Transaccion transaccion, CuentaBancaria origen, CuentaBancaria destino);
}
