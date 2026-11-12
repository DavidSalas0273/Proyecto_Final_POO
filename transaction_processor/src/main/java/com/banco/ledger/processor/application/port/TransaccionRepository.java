package com.banco.ledger.processor.application.port;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.Optional;

/**
 * Puerto de persistencia del ledger.
 * <ul>
 *   <li>El asiento del movimiento es inmutable: se inserta una sola vez.</li>
 *   <li>Cada cambio de estado se agrega a un historial (nunca se sobrescribe).</li>
 * </ul>
 */
public interface TransaccionRepository {

    boolean existe(TransactionId id);

    /** Registra un movimiento nuevo: su asiento en el ledger y su primer estado. */
    void guardar(TransactionEventRecord asiento);

    /** Reconstruye la transacción con su estado más reciente. */
    Optional<Transaccion> buscar(TransactionId id);

    /** Agrega al historial un nuevo estado de una transacción ya registrada. */
    void registrarCambioDeEstado(TransactionEventRecord asiento);
}
