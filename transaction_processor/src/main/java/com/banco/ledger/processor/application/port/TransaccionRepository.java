package com.banco.ledger.processor.application.port;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.Optional;

public interface TransaccionRepository {
    boolean existe(TransactionId id);

    void guardar(TransactionEventRecord asiento);

    Optional<Transaccion> buscar(TransactionId id);

    void registrarCambioDeEstado(TransactionEventRecord asiento);
}
