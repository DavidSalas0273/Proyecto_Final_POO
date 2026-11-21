package com.banco.ledger.processor.repository;

import com.banco.ledger.processor.dto.TransactionEventRecord;
import com.banco.ledger.processor.model.Transaccion;
import com.banco.ledger.processor.model.TransactionId;

import java.util.Optional;

public interface TransaccionRepository {
    boolean existe(TransactionId id);

    void guardar(TransactionEventRecord asiento);

    Optional<Transaccion> buscar(TransactionId id);

    void registrarCambioDeEstado(TransactionEventRecord asiento);
}
