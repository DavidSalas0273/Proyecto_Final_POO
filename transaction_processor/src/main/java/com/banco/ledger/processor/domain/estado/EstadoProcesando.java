package com.banco.ledger.processor.domain.estado;

/** PROCESANDO: se están aplicando las reglas; puede completarse o rechazarse. */
public final class EstadoProcesando implements EstadoTransaccion {

    public static final EstadoProcesando INSTANCIA = new EstadoProcesando();

    private EstadoProcesando() {
    }

    @Override
    public String codigo() {
        return "PROCESSING";
    }

    @Override
    public EstadoTransaccion completar() {
        return EstadoCompletada.INSTANCIA;
    }

    @Override
    public EstadoTransaccion rechazar() {
        return EstadoRechazada.INSTANCIA;
    }
}
