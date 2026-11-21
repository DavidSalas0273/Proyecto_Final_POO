package com.banco.ledger.notification.service;

import com.banco.ledger.notification.dto.TransactionEventRecord;
import com.banco.ledger.notification.model.PlantillaNotificaciones;
import com.banco.ledger.notification.rabbit.PublicadorComandosEmail;

import org.springframework.stereotype.Service;

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
