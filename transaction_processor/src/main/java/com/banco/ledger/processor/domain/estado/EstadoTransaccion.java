package com.banco.ledger.processor.domain.estado;

/**
 * Patrón State: cada estado concreto sobrescribe únicamente las transiciones que
 * permite. Las demás heredan el comportamiento por defecto (IllegalStateException),
 * evitando estructuras if-else / switch sobre el estado.
 *
 * RECIBIDA -> PROCESANDO -> COMPLETADA -> SOSPECHOSA
 *                       \-> RECHAZADA
 */
public interface EstadoTransaccion {

    /** Código publicado en el ledger (contrato de los eventos de Kafka). */
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

    private IllegalStateException transicionInvalida(String operacion) {
        return new IllegalStateException(
                "No se puede " + operacion + " una transacción en estado " + codigo());
    }
}
