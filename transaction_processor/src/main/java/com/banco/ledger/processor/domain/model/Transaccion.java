package com.banco.ledger.processor.domain.model;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.estado.EstadoRecibida;
import com.banco.ledger.processor.domain.estado.EstadoTransaccion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado rico de un movimiento del ledger: transferencia P2P o recarga de saldo.
 * Su ciclo de vida se gobierna con el patrón State: cada operación delega en el
 * estado actual, que decide la transición o lanza IllegalStateException.
 * La forma de mover el dinero la decide polimórficamente {@link TipoMovimiento}.
 */
public class Transaccion {

    public static final String MOTIVO_FONDOS_INSUFICIENTES = "INSUFFICIENT_FUNDS";

    private final TransactionId id;
    private final TipoMovimiento tipo;
    private final String origen;
    private final String destino;
    private final MontoDinero monto;
    private EstadoTransaccion estado;
    private String motivo;
    private Instant actualizadaEn;

    private Transaccion(TransactionId id, TipoMovimiento tipo, String origen, String destino,
                        MontoDinero monto, Instant actualizadaEn, EstadoTransaccion estado, String motivo) {
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        this.tipo = Objects.requireNonNull(tipo, "El tipo es obligatorio");
        this.origen = origen;
        this.destino = requerirTexto(destino, "destino");
        this.monto = Objects.requireNonNull(monto, "El monto es obligatorio");
        this.actualizadaEn = Objects.requireNonNull(actualizadaEn, "La fecha es obligatoria");
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.motivo = motivo;
        if (!monto.esPositivo()) {
            throw new IllegalArgumentException("El monto del movimiento debe ser mayor que cero");
        }
    }

    private Transaccion(TransactionId id, TipoMovimiento tipo, String origen, String destino,
                        MontoDinero monto, Instant recibidaEn) {
        this(id, tipo, origen, destino, monto, recibidaEn, EstadoRecibida.INSTANCIA, null);
    }

    /**
     * Reconstruye una transacción ya registrada en el ledger con su último estado, para
     * poder aplicarle nuevas transiciones (por ejemplo, marcarla como sospechosa).
     */
    public static Transaccion reconstituir(TransactionId id, TipoMovimiento tipo, String origen, String destino,
                                           MontoDinero monto, EstadoTransaccion estado, String motivo,
                                           Instant actualizadaEn) {
        return new Transaccion(id, tipo, origen, destino, monto, actualizadaEn, estado, motivo);
    }

    /** Transferencia P2P de {@code origen} a {@code destino}. */
    public static Transaccion recibirTransferencia(TransactionId id, String origen, String destino,
                                                   MontoDinero monto, Instant recibidaEn) {
        String origenValidado = requerirTexto(origen, "origen");
        if (origenValidado.equalsIgnoreCase(requerirTexto(destino, "destino"))) {
            throw new IllegalArgumentException("El origen y el destino no pueden coincidir");
        }
        return new Transaccion(id, TipoMovimiento.TRANSFER, origenValidado, destino, monto, recibidaEn);
    }

    /** Recarga de saldo en la cuenta de {@code titular}. */
    public static Transaccion recibirRecarga(TransactionId id, String titular, MontoDinero monto,
                                             Instant recibidaEn) {
        return new Transaccion(id, TipoMovimiento.DEPOSIT, null, titular, monto, recibidaEn);
    }

    public void iniciarProcesamiento(Instant ahora) {
        estado = estado.iniciarProcesamiento();
        actualizadaEn = ahora;
    }

    /**
     * Aplica el movimiento contable. La transición se valida antes de tocar los saldos
     * para no dejar cuentas modificadas si el movimiento no está en un estado válido.
     */
    public void ejecutar(Optional<CuentaBancaria> cuentaOrigen, CuentaBancaria cuentaDestino, Instant ahora) {
        exigirCuentasCorrectas(cuentaOrigen, cuentaDestino);
        EstadoTransaccion siguiente = estado.completar();
        tipo.aplicar(monto, cuentaOrigen, cuentaDestino);
        estado = siguiente;
        actualizadaEn = ahora;
    }

    public void ejecutarTransferencia(CuentaBancaria cuentaOrigen, CuentaBancaria cuentaDestino, Instant ahora) {
        ejecutar(Optional.of(cuentaOrigen), cuentaDestino, ahora);
    }

    public void ejecutarRecarga(CuentaBancaria cuentaDestino, Instant ahora) {
        ejecutar(Optional.empty(), cuentaDestino, ahora);
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

    /** true solo si el estado actual permite pasar a SOSPECHOSA (es decir, está COMPLETADA). */
    public boolean admiteMarcaDeSospecha() {
        return estado.admiteSospecha();
    }

    public void marcarComoSospechosa(String motivoSospecha, Instant ahora) {
        String motivoValidado = requerirTexto(motivoSospecha, "motivo de sospecha");
        estado = estado.marcarSospechosa();
        motivo = motivoValidado;
        actualizadaEn = ahora;
    }

    /** Proyecta el movimiento como evento del ledger, incluyendo los saldos resultantes. */
    public TransactionEventRecord aEvento(Optional<MontoDinero> saldoOrigen, MontoDinero saldoDestino) {
        return crearEvento(saldoOrigen.map(MontoDinero::valor).orElse(null),
                Objects.requireNonNull(saldoDestino, "El saldo de destino es obligatorio").valor());
    }

    /** Proyecta el movimiento como evento del ledger sin información de saldos. */
    public TransactionEventRecord aEvento() {
        return crearEvento(null, null);
    }

    private TransactionEventRecord crearEvento(BigDecimal saldoOrigen, BigDecimal saldoDestino) {
        return new TransactionEventRecord(id.valor(), tipo.name(), origen, destino, monto.valor(), monto.divisa(),
                estado.codigo(), motivo, saldoOrigen, saldoDestino, actualizadaEn);
    }

    public TransactionId id() {
        return id;
    }

    public TipoMovimiento tipo() {
        return tipo;
    }

    public Optional<String> origen() {
        return Optional.ofNullable(origen);
    }

    public String destino() {
        return destino;
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

    private void exigirCuentasCorrectas(Optional<CuentaBancaria> cuentaOrigen, CuentaBancaria cuentaDestino) {
        boolean origenCorrecto = origen()
                .map(usuario -> cuentaOrigen.filter(cuenta -> cuenta.perteneceA(usuario)).isPresent())
                .orElse(cuentaOrigen.isEmpty());
        if (!origenCorrecto || cuentaDestino == null || !cuentaDestino.perteneceA(destino)) {
            throw new IllegalArgumentException("Las cuentas no corresponden a los usuarios del movimiento " + id.valor());
        }
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El " + campo + " no puede ser vacío");
        }
        return valor.trim();
    }
}
