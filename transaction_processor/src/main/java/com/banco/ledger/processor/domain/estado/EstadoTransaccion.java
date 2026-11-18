package com.banco.ledger.processor.domain.estado;

public interface EstadoTransaccion {
    String codigo();

    default EstadoTransaccion iniciarProcesamiento() {
        throw transicionInvalida("iniciar el procesamiento de");
    }

    default EstadoTransaccion completar() {
        throw transicionInvalida("completar");
    }

    default EstadoTransaccion rechazar() {
        throw transicionInvalida("rechazar");
    }

    default EstadoTransaccion marcarSospechosa() {
        throw transicionInvalida("marcar como sospechosa");
    }

    default boolean admiteSospecha() {
        return false;
    }

    private IllegalStateException transicionInvalida(String operacion) {
        return new IllegalStateException(
                "No se puede " + operacion + " una transacción en estado " + codigo());
    }
}
