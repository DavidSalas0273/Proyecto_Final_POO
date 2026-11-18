package com.banco.ledger.api.domain.usuario;

/** Ya hay una cuenta registrada con ese usuario, correo o documento. */
public class UsuarioYaExisteException extends RuntimeException {

    private final String campo;

    public UsuarioYaExisteException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    /** Campo del formulario que causó el conflicto (username, email o document). */
    public String campo() {
        return campo;
    }
}
