package com.banco.ledger.fraud.domain.regla;

import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.banco.ledger.fraud.domain.model.HistorialTransacciones;

import java.util.Optional;

@FunctionalInterface
public interface ReglaDeteccionFraude {
    Optional<String> evaluar(TransactionEventRecord evento, HistorialTransacciones historial);
}
