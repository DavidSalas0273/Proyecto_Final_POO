package com.banco.ledger.notification.domain;

import com.banco.ledger.notification.domain.contract.EmailCommandRecord;
import com.banco.ledger.notification.domain.contract.TransactionEventRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Traduce un evento del ledger en los emails para emisor y receptor.
 * Ej.: { "to": "user_a@email.com", "body": "Envió $50 a B" }.
 */
public class PlantillaNotificaciones {

    private static final Map<String, String> SIMBOLOS = Map.of("USD", "$", "EUR", "€", "COP", "COL$");

    private final String dominioEmail;

    public PlantillaNotificaciones(String dominioEmail) {
        if (dominioEmail == null || dominioEmail.isBlank()) {
            throw new IllegalArgumentException("El dominio de email es obligatorio");
        }
        this.dominioEmail = dominioEmail.trim();
    }

    public List<EmailCommandRecord> paraTransferencia(TransactionEventRecord evento) {
        String monto = formatear(evento.amount(), evento.currency());
        return List.of(
                new EmailCommandRecord(direccionDe(evento.fromUser()), "Envió " + monto + " a " + evento.toUser()),
                new EmailCommandRecord(direccionDe(evento.toUser()), "Recibió " + monto + " de " + evento.fromUser()));
    }

    String direccionDe(String usuario) {
        return "user_" + usuario.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_") + "@" + dominioEmail;
    }

    private static String formatear(BigDecimal monto, String divisa) {
        return SIMBOLOS.getOrDefault(divisa, divisa + " ") + monto.stripTrailingZeros().toPlainString();
    }
}
