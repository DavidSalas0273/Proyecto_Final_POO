package com.banco.ledger.api.service;

@FunctionalInterface
public interface TxIdGenerator {
    String nuevoId();
}
