package com.banco.ledger.processor.application.port;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.model.TransactionId;

/** Puerto de persistencia del histórico inmutable del ledger (un asiento por movimiento). */
public interface TransaccionRepository {

    boolean existe(TransactionId id);

    void guardar(TransactionEventRecord asiento);
}
