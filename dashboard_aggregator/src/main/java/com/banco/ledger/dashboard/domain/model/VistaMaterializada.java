package com.banco.ledger.dashboard.domain.model;

import com.banco.ledger.dashboard.domain.contract.FraudAlertRecord;
import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Estado en memoria del agregador: proyección de transactions_log + fraud_alerts.
 * <ul>
 *   <li>Movimientos: lista de transacciones/recargas con su marca de fraude.</li>
 *   <li>Cuentas: saldo vigente de cada usuario, tomado de los eventos del ledger.</li>
 * </ul>
 * Como los dos topics se consumen en paralelo, una alerta puede llegar antes que su
 * transacción: se guarda como pendiente y se aplica cuando llega el evento.
 */
public class VistaMaterializada {

    private final int capacidad;
    private final LinkedHashMap<String, TransaccionVista> transacciones = new LinkedHashMap<>();
    private final Map<String, String> alertasPendientes = new HashMap<>();
    private final Map<String, SaldoCuenta> cuentas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public VistaMaterializada(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva");
        }
        this.capacidad = capacidad;
    }

    public synchronized TransaccionVista aplicarTransaccion(TransactionEventRecord evento) {
        TransaccionVista base = TransaccionVista.desde(evento);
        TransaccionVista vista = Optional.ofNullable(alertasPendientes.remove(evento.txId()))
                .or(() -> motivoSospechaExistente(evento.txId()))
                .map(base::marcarSospechosa)
                .orElse(base);
        transacciones.put(vista.txId(), vista);
        actualizarSaldo(evento.fromUser(), evento.fromBalance(), evento);
        actualizarSaldo(evento.toUser(), evento.toBalance(), evento);
        recortar();
        return vista;
    }

    public synchronized Optional<TransaccionVista> aplicarAlerta(FraudAlertRecord alerta) {
        Optional<TransaccionVista> actualizada = Optional.ofNullable(transacciones.get(alerta.txId()))
                .map(vista -> vista.marcarSospechosa(alerta.reason()));
        actualizada.ifPresentOrElse(
                vista -> transacciones.put(vista.txId(), vista),
                () -> alertasPendientes.put(alerta.txId(), alerta.reason()));
        return actualizada;
    }

    /** Copia inmutable con los movimientos más recientes primero. */
    public synchronized List<TransaccionVista> instantanea() {
        List<TransaccionVista> copia = new ArrayList<>(transacciones.values());
        Collections.reverse(copia);
        return List.copyOf(copia);
    }

    /** Copia inmutable de los saldos, ordenada por usuario. */
    public synchronized List<SaldoCuenta> cuentas() {
        return List.copyOf(cuentas.values());
    }

    private void actualizarSaldo(String usuario, BigDecimal saldo, TransactionEventRecord evento) {
        Optional.ofNullable(usuario)
                .filter(u -> saldo != null)
                .map(u -> new SaldoCuenta(u, saldo, evento.currency(), evento.timestamp()))
                .ifPresent(nuevo -> cuentas.merge(nuevo.usuario(), nuevo, SaldoCuenta::masRecienteEntre));
    }

    private Optional<String> motivoSospechaExistente(String txId) {
        return Optional.ofNullable(transacciones.get(txId))
                .filter(TransaccionVista::suspicious)
                .map(TransaccionVista::fraudReason);
    }

    private void recortar() {
        Iterator<String> masAntiguas = transacciones.keySet().iterator();
        while (transacciones.size() > capacidad && masAntiguas.hasNext()) {
            masAntiguas.next();
            masAntiguas.remove();
        }
    }
}
