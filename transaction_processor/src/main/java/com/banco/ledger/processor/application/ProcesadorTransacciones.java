package com.banco.ledger.processor.application;

import com.banco.ledger.processor.application.port.CuentaRepository;
import com.banco.ledger.processor.application.port.PublicadorEventosLedger;
import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.contract.TransferCommandRecord;
import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.PoliticaAperturaCuentas;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import com.banco.ledger.processor.domain.regla.CadenaValidacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Orquestador del caso de uso "procesar transferencia".
 * Comando -> reglas de dominio -> persistencia (PostgreSQL) -> evento (Kafka).
 * Solo conoce puertos (interfaces); los adaptadores se inyectan por constructor.
 */
@Service
public class ProcesadorTransacciones {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorTransacciones.class);

    private final TransaccionRepository transacciones;
    private final CuentaRepository cuentas;
    private final PublicadorEventosLedger publicador;
    private final CadenaValidacion validaciones;
    private final PoliticaAperturaCuentas politicaApertura;
    private final Clock clock;

    public ProcesadorTransacciones(TransaccionRepository transacciones,
                                   CuentaRepository cuentas,
                                   PublicadorEventosLedger publicador,
                                   CadenaValidacion validaciones,
                                   PoliticaAperturaCuentas politicaApertura,
                                   Clock clock) {
        this.transacciones = transacciones;
        this.cuentas = cuentas;
        this.publicador = publicador;
        this.validaciones = validaciones;
        this.politicaApertura = politicaApertura;
        this.clock = clock;
    }

    @Transactional
    public Optional<TransactionEventRecord> procesar(TransferCommandRecord comando) {
        TransactionId id = new TransactionId(comando.txId());
        if (transacciones.existe(id)) {
            log.warn("Comando duplicado ignorado (idempotencia): {}", id.valor());
            return Optional.empty();
        }

        Transaccion transaccion = Transaccion.recibir(id, comando.fromUser(), comando.toUser(),
                new MontoDinero(comando.amount(), comando.currency()), comando.requestedAt());
        transaccion.iniciarProcesamiento(clock.instant());

        CuentaBancaria origen = obtenerOAbrirCuenta(comando.fromUser());
        CuentaBancaria destino = obtenerOAbrirCuenta(comando.toUser());

        validaciones.primerMotivoDeRechazo(transaccion, origen, destino).ifPresentOrElse(
                motivo -> transaccion.rechazar(motivo, clock.instant()),
                () -> transaccion.ejecutarTransferencia(origen, destino, clock.instant()));

        cuentas.guardar(origen);
        cuentas.guardar(destino);
        transacciones.guardar(transaccion);

        TransactionEventRecord evento = transaccion.aEvento();
        publicador.publicar(evento);
        return Optional.of(evento);
    }

    private CuentaBancaria obtenerOAbrirCuenta(String titular) {
        return cuentas.buscarPorTitular(titular)
                .orElseGet(() -> politicaApertura.abrirCuentaPara(titular));
    }
}
