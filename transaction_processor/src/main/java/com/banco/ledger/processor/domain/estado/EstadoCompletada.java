package com.banco.ledger.processor.domain.estado;

/** COMPLETADA: movimiento contable aplicado; solo puede marcarse como sospechosa. */
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
