package com.banco.ledger.notification.domain.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Comando inmutable de envío de email publicado en la Work Queue email_queue. */
public record EmailCommandRecord(@JsonProperty("to") String to,
                                 @JsonProperty("body") String body) {

    public EmailCommandRecord {
        if (to == null || to.isBlank() || !to.contains("@")) {
            throw new IllegalArgumentException("Destinatario de email inválido: " + to);
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("El cuerpo del email no puede ser vacío");
        }
    }
}
