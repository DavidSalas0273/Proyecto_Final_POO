package com.banco.ledger.processor.estado;

public final class EstadoRechazada implements EstadoTransaccion {
    public static final EstadoRechazada INSTANCIA = new EstadoRechazada();

    private EstadoRechazada() {
    }

    @Override
    public String codigo() {
        return "REJECTED";
    }
}
