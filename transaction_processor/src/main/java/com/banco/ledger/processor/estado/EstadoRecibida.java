package com.banco.ledger.processor.estado;

public final class EstadoRecibida implements EstadoTransaccion {
    public static final EstadoRecibida INSTANCIA = new EstadoRecibida();

    private EstadoRecibida() {
    }

    @Override
    public String codigo() {
        return "RECEIVED";
    }

    @Override
    public EstadoTransaccion iniciarProcesamiento() {
        return EstadoProcesando.INSTANCIA;
    }
}
