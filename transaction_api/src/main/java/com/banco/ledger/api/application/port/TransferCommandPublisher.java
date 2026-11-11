package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.ComandoLedger;

/** Puerto de salida: desacopla la lógica de negocio del SDK de RabbitMQ (DIP). */
public interface TransferCommandPublisher {

    void publicar(ComandoLedger comando);
}
