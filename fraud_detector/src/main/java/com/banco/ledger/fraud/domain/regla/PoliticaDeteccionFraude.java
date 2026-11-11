package com.banco.ledger.fraud.domain.regla;

import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.banco.ledger.fraud.domain.model.HistorialTransacciones;

import java.util.List;
import java.util.Optional;

/** Conjunto ordenado de estrategias de detección: devuelve el primer motivo de alerta. */
public class PoliticaDeteccionFraude {

    private final List<ReglaDeteccionFraude> reglas;

    public PoliticaDeteccionFraude(List<ReglaDeteccionFraude> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    public Optional<String> primerMotivoDeAlerta(TransactionEventRecord evento, HistorialTransacciones historial) {
        return reglas.stream()
                .map(regla -> regla.evaluar(evento, historial))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
