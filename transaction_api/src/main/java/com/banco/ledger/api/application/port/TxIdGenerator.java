package com.banco.ledger.api.application.port;

/** Puerto para generar identificadores únicos de transacción. */
@FunctionalInterface
public interface TxIdGenerator {

    String nuevoId();
}
