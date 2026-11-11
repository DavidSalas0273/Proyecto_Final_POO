package com.banco.ledger.processor.domain.model;

/** Identificador inmutable de una transacción (tx_id). */
public record TransactionId(String valor) {

    public TransactionId {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El tx_id no puede ser nulo ni vacío");
        }
        valor = valor.trim();
    }
}
