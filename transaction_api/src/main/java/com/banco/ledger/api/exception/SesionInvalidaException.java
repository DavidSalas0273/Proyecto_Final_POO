package com.banco.ledger.api.exception;

public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException() {
        super("Tu sesión venció. Vuelve a ingresar.");
    }
}
