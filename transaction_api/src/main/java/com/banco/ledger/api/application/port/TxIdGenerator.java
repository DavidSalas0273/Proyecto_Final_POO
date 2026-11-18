package com.banco.ledger.api.application.port;

@FunctionalInterface
public interface TxIdGenerator {
    String nuevoId();
}
