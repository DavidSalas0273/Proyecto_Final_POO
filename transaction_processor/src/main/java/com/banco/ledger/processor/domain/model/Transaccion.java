package com.banco.ledger.processor.domain.model;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.estado.EstadoRecibida;
import com.banco.ledger.processor.domain.estado.EstadoTransaccion;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado rico de una transferencia P2P.
 * Su ciclo de vida se gobierna con el patrón State: cada operación delega en el
 * estado actual, que decide la transición o lanza IllegalStateException.
 */
public class Transaccion {

    public static final String MOTIVO_FONDOS_INSUFICIENTES = "INSUFFICIENT_FUNDS";

    private final TransactionId id;
    private final String origen;
    private final String destino;
    private final MontoDinero monto;
    private EstadoTransaccion estado;
    private String motivo;
    private Instant actualizadaEn;

    private Transaccion(TransactionId id, String origen, String destino, MontoDinero monto, Instant recibidaEn) {
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        this.origen = requerirTexto(origen, "origen");
        this.destino = requerirTexto(destino, "destino");
        this.monto = Objects.requireNonNull(monto, "El monto es obligatorio");
        this.actualizadaEn = Objects.requireNonNull(recibidaEn, "La fecha de recepción es obligatoria");
        if (!monto.esPositivo()) {
            throw new IllegalArgumentException("El monto de la transacción debe ser mayor que cero");
        }
        if (this.origen.equalsIgnoreCase(this.destino)) {
            throw new IllegalArgumentException("El origen y el destino no pueden coincidir");
        }
        this.estado = EstadoRecibida.INSTANCIA;
    }

    public static Transaccion recibir(TransactionId id, String origen, String destino,
                                      MontoDinero monto, Instant recibidaEn) {
        return new Transaccion(id, origen, destino, monto, recibidaEn);
    }

    public void iniciarProcesamiento(Instant ahora) {
        estado = estado.iniciarProcesamiento();
        actualizadaEn = ahora;
    }

    /**
     * Ejecuta el movimiento contable. La transición se valida antes de tocar los saldos
     * para no dejar cuentas modificadas si la transacción no está en un estado válido.
     */
    public void ejecutarTransferencia(CuentaBancaria cuentaOrigen, CuentaBancaria cuentaDestino, Instant ahora) {
        exigirTitular(cuentaOrigen, origen);
        exigirTitular(cuentaDestino, destino);
        EstadoTransaccion siguiente = estado.completar();
        cuentaOrigen.debitar(monto);
        cuentaDestino.acreditar(monto);
        estado = siguiente;
        actualizadaEn = ahora;
    }

    public void rechazar(String motivoRechazo, Instant ahora) {
        String motivoValidado = requerirTexto(motivoRechazo, "motivo de rechazo");
        estado = estado.rechazar();
        motivo = motivoValidado;
        actualizadaEn = ahora;
    }

    public void rechazarPorFondosInsuficientes(Instant ahora) {
        rechazar(MOTIVO_FONDOS_INSUFICIENTES, ahora);
    }

    public void marcarComoSospechosa(String motivoSospecha, Instant ahora) {
        String motivoValidado = requerirTexto(motivoSospecha, "motivo de sospecha");
        estado = estado.marcarSospechosa();
        motivo = motivoValidado;
        actualizadaEn = ahora;
    }

    /** Proyecta el estado actual del agregado como evento inmutable del ledger. */
    public TransactionEventRecord aEvento() {
        return new TransactionEventRecord(id.valor(), origen, destino, monto.valor(), monto.divisa(),
                estado.codigo(), motivo, actualizadaEn);
    }

    public TransactionId id() {
        return id;
    }

    public MontoDinero monto() {
        return monto;
    }

    public String estadoActual() {
        return estado.codigo();
    }

    public Optional<String> motivo() {
        return Optional.ofNullable(motivo);
    }

    private static void exigirTitular(CuentaBancaria cuenta, String usuario) {
        if (cuenta == null || !cuenta.perteneceA(usuario)) {
            throw new IllegalArgumentException("La cuenta no corresponde al usuario " + usuario);
        }
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El " + campo + " no puede ser vacío");
        }
        return valor.trim();
    }
}
