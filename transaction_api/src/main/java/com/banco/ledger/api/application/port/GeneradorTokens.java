package com.banco.ledger.api.application.port;

/** Puerto: genera tokens de sesión impredecibles. */
@FunctionalInterface
public interface GeneradorTokens {

    String nuevoToken();
}
