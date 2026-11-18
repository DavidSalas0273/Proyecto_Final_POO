package com.banco.ledger.api.domain.usuario;

public class UsuarioYaExisteException extends RuntimeException {
    private final String campo;

    public UsuarioYaExisteException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
