package com.banco.ledger.api.domain.usuario;

public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException() {
        super("Tu sesión venció. Vuelve a ingresar.");
    }
}
