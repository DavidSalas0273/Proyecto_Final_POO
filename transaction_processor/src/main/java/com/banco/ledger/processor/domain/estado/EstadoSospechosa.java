package com.banco.ledger.processor.domain.estado;

public final class EstadoSospechosa implements EstadoTransaccion {
    public static final EstadoSospechosa INSTANCIA = new EstadoSospechosa();

    private EstadoSospechosa() {
    }

    @Override
    public String codigo() {
        return "SUSPICIOUS";
    }
}
