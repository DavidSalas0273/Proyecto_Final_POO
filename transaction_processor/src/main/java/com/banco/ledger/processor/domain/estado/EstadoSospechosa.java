package com.banco.ledger.processor.domain.estado;

/** SOSPECHOSA: estado final de auditoría, bloquea cualquier transición. */
public final class EstadoSospechosa implements EstadoTransaccion {

    public static final EstadoSospechosa INSTANCIA = new EstadoSospechosa();

    private EstadoSospechosa() {
    }

    @Override
    public String codigo() {
        return "SUSPICIOUS";
    }
}
