package com.banco.ledger.processor.domain.estado;

public final class EstadoCompletada implements EstadoTransaccion {
    public static final EstadoCompletada INSTANCIA = new EstadoCompletada();

    private EstadoCompletada() {
    }

    @Override
    public String codigo() {
        return "COMPLETED";
    }

    @Override
    public EstadoTransaccion marcarSospechosa() {
        return EstadoSospechosa.INSTANCIA;
    }

    @Override
    public boolean admiteSospecha() {
        return true;
    }
}
