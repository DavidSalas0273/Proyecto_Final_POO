package com.banco.ledger.api.domain.usuario;

/** Usuario o contraseña incorrectos. El mensaje no revela cuál de los dos falló. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos");
    }
}
