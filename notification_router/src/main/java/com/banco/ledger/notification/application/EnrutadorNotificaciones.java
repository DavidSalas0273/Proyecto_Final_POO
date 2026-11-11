package com.banco.ledger.notification.application;

import com.banco.ledger.notification.application.port.PublicadorComandosEmail;
import com.banco.ledger.notification.domain.PlantillaNotificaciones;
import com.banco.ledger.notification.domain.contract.TransactionEventRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

/** Caso de uso: por cada evento COMPLETED, encolar las notificaciones correspondientes. */
@Service
public class EnrutadorNotificaciones {

    private final PlantillaNotificaciones plantilla;
    private final PublicadorComandosEmail publicador;

    public EnrutadorNotificaciones(PlantillaNotificaciones plantilla, PublicadorComandosEmail publicador) {
        this.plantilla = plantilla;
        this.publicador = publicador;
    }

    public void enrutar(TransactionEventRecord evento) {
        Stream.of(evento)
                .filter(TransactionEventRecord::estaCompletada)
                .map(plantilla::paraTransferencia)
                .flatMap(List::stream)
                .forEach(publicador::publicar);
    }
}
