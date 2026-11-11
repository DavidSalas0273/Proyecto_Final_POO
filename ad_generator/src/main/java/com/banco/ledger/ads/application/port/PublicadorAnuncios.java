package com.banco.ledger.ads.application.port;

import com.banco.ledger.ads.domain.contract.EmailCommandRecord;

/** Puerto de salida hacia la cola de emails (RabbitMQ). */
public interface PublicadorAnuncios {

    void publicar(EmailCommandRecord anuncio);
}
