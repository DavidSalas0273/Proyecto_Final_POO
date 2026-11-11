package com.banco.ledger.processor.application.port;

import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;

/** Puerto de persistencia del histórico inmutable del ledger. */
public interface TransaccionRepository {

    boolean existe(TransactionId id);

    void guardar(Transaccion transaccion);
}
