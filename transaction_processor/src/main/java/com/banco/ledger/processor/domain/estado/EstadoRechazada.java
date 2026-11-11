package com.banco.ledger.processor.domain.estado;

/** RECHAZADA: estado final, bloquea cualquier transición. */
public final class EstadoRechazada implements EstadoTransaccion {

    public static final EstadoRechazada INSTANCIA = new EstadoRechazada();

    private EstadoRechazada() {
    }

    @Override
    public String codigo() {
        return "REJECTED";
    }
}
