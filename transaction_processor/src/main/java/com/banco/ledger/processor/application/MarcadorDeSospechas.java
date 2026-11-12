package com.banco.ledger.processor.application;

import com.banco.ledger.processor.application.port.PublicadorEventosLedger;
import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.FraudAlertRecord;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Caso de uso: cuando fraud_detector publica una alerta, la transacción pasa de
 * COMPLETADA a SOSPECHOSA (patrón State), el cambio queda en el historial de
 * estados (PostgreSQL) y se publica en el ledger como un nuevo evento.
 */
@Service
public class MarcadorDeSospechas {

    private static final Logger log = LoggerFactory.getLogger(MarcadorDeSospechas.class);

    private final TransaccionRepository transacciones;
    private final PublicadorEventosLedger publicador;
    private final Clock clock;

    public MarcadorDeSospechas(TransaccionRepository transacciones, PublicadorEventosLedger publicador,
                               Clock clock) {
        this.transacciones = transacciones;
        this.publicador = publicador;
        this.clock = clock;
    }

    /**
     * @throws IllegalStateException si la transacción todavía no está registrada; el
     *                               consumidor de Kafka reintenta el mensaje.
     */
    @Transactional
    public Optional<TransactionEventRecord> marcar(FraudAlertRecord alerta) {
        Transaccion transaccion = transacciones.buscar(new TransactionId(alerta.txId()))
                .orElseThrow(() -> new IllegalStateException(
                        "La transacción " + alerta.txId() + " aún no está registrada en el ledger"));

        return Optional.of(transaccion)
                .filter(Transaccion::admiteMarcaDeSospecha)
                .map(tx -> aplicarSospecha(tx, alerta.reason()))
                .or(() -> {
                    log.info("Alerta de {} ignorada: la transacción está en estado {}",
                            alerta.txId(), transaccion.estadoActual());
                    return Optional.empty();
                });
    }

    private TransactionEventRecord aplicarSospecha(Transaccion transaccion, String motivo) {
        transaccion.marcarComoSospechosa(motivo, clock.instant());
        TransactionEventRecord evento = transaccion.aEvento();
        transacciones.registrarCambioDeEstado(evento);
        publicador.publicar(evento);
        log.warn("Transacción {} marcada como SOSPECHOSA ({})", evento.txId(), motivo);
        return evento;
    }
}
