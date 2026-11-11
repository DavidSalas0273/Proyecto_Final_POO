package com.banco.ledger.fraud.application;

import com.banco.ledger.fraud.application.port.PublicadorAlertasFraude;
import com.banco.ledger.fraud.application.port.RepositorioAlertasFraude;
import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;
import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.banco.ledger.fraud.domain.model.HistorialTransacciones;
import com.banco.ledger.fraud.domain.regla.PoliticaDeteccionFraude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;

/**
 * Caso de uso: analizar cada evento COMPLETED del ledger, mantener el estado del
 * flujo y, si alguna estrategia lo marca, auditar en PostgreSQL y publicar la alerta.
 */
@Service
public class DetectorFraude {

    private static final Logger log = LoggerFactory.getLogger(DetectorFraude.class);

    private final PoliticaDeteccionFraude politica;
    private final HistorialTransacciones historial;
    private final RepositorioAlertasFraude repositorio;
    private final PublicadorAlertasFraude publicador;
    private final Clock clock;

    public DetectorFraude(PoliticaDeteccionFraude politica, HistorialTransacciones historial,
                          RepositorioAlertasFraude repositorio, PublicadorAlertasFraude publicador, Clock clock) {
        this.politica = politica;
        this.historial = historial;
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.clock = clock;
    }

    public Optional<FraudAlertRecord> analizar(TransactionEventRecord evento) {
        return Optional.of(evento)
                .filter(TransactionEventRecord::estaCompletada)
                .flatMap(this::evaluar);
    }

    private Optional<FraudAlertRecord> evaluar(TransactionEventRecord evento) {
        historial.registrar(evento.fromUser(), evento.timestamp());
        Optional<FraudAlertRecord> alerta = politica.primerMotivoDeAlerta(evento, historial)
                .map(motivo -> FraudAlertRecord.de(evento, motivo, clock.instant()));
        alerta.ifPresentOrElse(this::registrarYPublicar,
                () -> log.info("Transacción {} ({} {}) sin indicios de fraude",
                        evento.txId(), evento.amount(), evento.currency()));
        return alerta;
    }

    private void registrarYPublicar(FraudAlertRecord alerta) {
        log.warn("ALERTA DE FRAUDE {} -> {}", alerta.txId(), alerta.reason());
        repositorio.registrar(alerta);
        publicador.publicar(alerta);
    }
}
