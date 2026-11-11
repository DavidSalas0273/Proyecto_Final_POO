package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.TransferCommandRecord;

/** Puerto de salida: desacopla la lógica de negocio del SDK de RabbitMQ (DIP). */
public interface TransferCommandPublisher {

    void publicar(TransferCommandRecord comando);
}
