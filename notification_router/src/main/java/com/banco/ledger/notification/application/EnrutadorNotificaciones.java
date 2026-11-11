package com.banco.ledger.notification.application;

import com.banco.ledger.notification.application.port.PublicadorComandosEmail;
import com.banco.ledger.notification.domain.PlantillaNotificaciones;
import com.banco.ledger.notification.domain.contract.TransactionEventRecord;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: por cada evento del ledger, encolar las notificaciones que le
 * correspondan (envíos, recargas y operaciones denegadas).
 */
@Service
public class EnrutadorNotificaciones {

    private final PlantillaNotificaciones plantilla;
    private final PublicadorComandosEmail publicador;

    public EnrutadorNotificaciones(PlantillaNotificaciones plantilla, PublicadorComandosEmail publicador) {
        this.plantilla = plantilla;
        this.publicador = publicador;
    }

    public void enrutar(TransactionEventRecord evento) {
        plantilla.para(evento).forEach(publicador::publicar);
    }
}
