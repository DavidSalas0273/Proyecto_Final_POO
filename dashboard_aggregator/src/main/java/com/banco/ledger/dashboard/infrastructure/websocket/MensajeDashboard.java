package com.banco.ledger.dashboard.infrastructure.websocket;

import com.banco.ledger.dashboard.domain.model.TransaccionVista;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Mensaje enviado al cliente: SNAPSHOT (lista completa) o UPSERT (una transacción). */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MensajeDashboard(String type, List<TransaccionVista> transactions, TransaccionVista transaction) {
}
