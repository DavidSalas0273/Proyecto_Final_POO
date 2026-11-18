package com.banco.ledger.processor.domain.estado;

public final class EstadoRechazada implements EstadoTransaccion {
    public static final EstadoRechazada INSTANCIA = new EstadoRechazada();

    private EstadoRechazada() {
    }

    @Override
    public String codigo() {
        return "REJECTED";
    }
}
