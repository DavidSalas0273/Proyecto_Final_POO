package com.banco.ledger.notification.application.port;

import com.banco.ledger.notification.domain.contract.EmailCommandRecord;

/** Puerto de salida hacia la cola de emails (RabbitMQ). */
public interface PublicadorComandosEmail {

    void publicar(EmailCommandRecord comando);
}
