package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.List;
import java.util.Optional;

/**
 * Agrupa las estrategias de validación en un orden definido y devuelve el primer
 * motivo de rechazo (evaluación perezosa: se detiene en la primera regla que falla).
 */
public class CadenaValidacion {

    private final List<ReglaValidacionTransaccion> reglas;

    public CadenaValidacion(List<ReglaValidacionTransaccion> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    public Optional<String> primerMotivoDeRechazo(Transaccion transaccion, CuentaBancaria origen,
                                                   CuentaBancaria destino) {
        return reglas.stream()
                .map(regla -> regla.evaluar(transaccion, origen, destino))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
