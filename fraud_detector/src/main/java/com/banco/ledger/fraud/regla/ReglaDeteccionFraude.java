package com.banco.ledger.fraud.regla;

import com.banco.ledger.fraud.dto.TransactionEventRecord;
import com.banco.ledger.fraud.model.HistorialTransacciones;

import java.util.Optional;

@FunctionalInterface
public interface ReglaDeteccionFraude {
    Optional<String> evaluar(TransactionEventRecord evento, HistorialTransacciones historial);
}
