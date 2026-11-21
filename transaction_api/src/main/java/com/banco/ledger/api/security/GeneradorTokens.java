package com.banco.ledger.api.security;

@FunctionalInterface
public interface GeneradorTokens {
    String nuevoToken();
}
