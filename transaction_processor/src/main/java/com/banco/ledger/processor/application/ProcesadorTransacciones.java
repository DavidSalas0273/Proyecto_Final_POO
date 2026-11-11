package com.banco.ledger.processor.application;

import com.banco.ledger.processor.application.port.CuentaRepository;
import com.banco.ledger.processor.application.port.PublicadorEventosLedger;
import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.ComandoLedger;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.model.CuentaBancaria;
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
 * Orquestador del caso de uso "procesar movimiento" (transferencia o recarga).
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
    public Optional<TransactionEventRecord> procesar(ComandoLedger comando) {
        TransactionId id = new TransactionId(comando.txId());
        if (transacciones.existe(id)) {
            log.warn("Comando duplicado ignorado (idempotencia): {}", id.valor());
            return Optional.empty();
        }

        Transaccion transaccion = comando.crearTransaccion();
        transaccion.iniciarProcesamiento(clock.instant());

        Optional<CuentaBancaria> origen = transaccion.origen().map(this::obtenerOAbrirCuenta);
        CuentaBancaria destino = obtenerOAbrirCuenta(transaccion.destino());

        validaciones.primerMotivoDeRechazo(transaccion, origen, destino).ifPresentOrElse(
                motivo -> transaccion.rechazar(motivo, clock.instant()),
                () -> transaccion.ejecutar(origen, destino, clock.instant()));

        origen.ifPresent(cuentas::guardar);
        cuentas.guardar(destino);

        TransactionEventRecord evento = transaccion.aEvento(origen.map(CuentaBancaria::saldo), destino.saldo());
        transacciones.guardar(evento);
        publicador.publicar(evento);
        return Optional.of(evento);
    }

    private CuentaBancaria obtenerOAbrirCuenta(String titular) {
        return cuentas.buscarPorTitular(titular)
                .orElseGet(() -> politicaApertura.abrirCuentaPara(titular));
    }
}
