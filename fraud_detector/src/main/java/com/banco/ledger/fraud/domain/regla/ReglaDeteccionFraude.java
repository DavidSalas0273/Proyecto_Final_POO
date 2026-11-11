package com.banco.ledger.fraud.domain.regla;

import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.banco.ledger.fraud.domain.model.HistorialTransacciones;

import java.util.Optional;

/** Patrón Strategy: política intercambiable de detección de fraude. */
@FunctionalInterface
public interface ReglaDeteccionFraude {

    /** Devuelve el motivo de la alerta, o vacío si el evento no es sospechoso. */
    Optional<String> evaluar(TransactionEventRecord evento, HistorialTransacciones historial);
}
