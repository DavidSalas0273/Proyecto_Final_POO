package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.List;
import java.util.Optional;

public class CadenaValidacion {
    private final List<ReglaValidacionTransaccion> reglas;

    public CadenaValidacion(List<ReglaValidacionTransaccion> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    public Optional<String> primerMotivoDeRechazo(Transaccion transaccion, Optional<CuentaBancaria> origen,
                                                   CuentaBancaria destino) {
        return reglas.stream()
                .map(regla -> regla.evaluar(transaccion, origen, destino))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
