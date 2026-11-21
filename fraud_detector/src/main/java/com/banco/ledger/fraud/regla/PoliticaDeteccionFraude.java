package com.banco.ledger.fraud.regla;

import com.banco.ledger.fraud.dto.TransactionEventRecord;
import com.banco.ledger.fraud.model.HistorialTransacciones;

import java.util.List;
import java.util.Optional;

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
