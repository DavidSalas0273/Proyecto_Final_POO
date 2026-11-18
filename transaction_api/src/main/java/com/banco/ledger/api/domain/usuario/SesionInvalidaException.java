package com.banco.ledger.api.domain.usuario;

/** El token no existe o la sesión ya venció. */
public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException() {
        super("Tu sesión venció. Vuelve a ingresar.");
    }
}
