package com.banco.ledger.processor.domain.estado;

/** RECIBIDA: el comando llegó; solo puede pasar a PROCESANDO. */
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
