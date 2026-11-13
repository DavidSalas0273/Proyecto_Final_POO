package com.banco.ledger.notification.domain;

import com.banco.ledger.notification.domain.contract.EmailCommandRecord;
import com.banco.ledger.notification.domain.contract.TransactionEventRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Traduce un evento del ledger en los emails correspondientes. Cada combinación
 * "TIPO:ESTADO" tiene su propia plantilla (tabla de estrategias, sin switch):
 * <ul>
 *   <li>TRANSFER:COMPLETED -> emisor ("Envió $50 a B") y receptor ("Recibió $50 de A").</li>
 *   <li>DEPOSIT:COMPLETED  -> titular ("Recargó $100 en su cuenta. Saldo disponible: $100").</li>
 *   <li>TRANSFER:REJECTED / DEPOSIT:REJECTED -> aviso de operación denegada.</li>
 * </ul>
 */
public class PlantillaNotificaciones {

    private static final Map<String, String> SIMBOLOS = Map.of("USD", "$", "EUR", "€", "COP", "COL$");
    private static final Map<String, String> MOTIVOS = Map.of(
            "INSUFFICIENT_FUNDS", "fondos insuficientes",
            "CURRENCY_MISMATCH", "la divisa no coincide con la de la cuenta",
            "AMOUNT_LIMIT_EXCEEDED", "supera el límite por operación");

    private final String dominioEmail;
    private final Map<String, Function<TransactionEventRecord, List<EmailCommandRecord>>> plantillas;

    public PlantillaNotificaciones(String dominioEmail) {
        if (dominioEmail == null || dominioEmail.isBlank()) {
            throw new IllegalArgumentException("El dominio de email es obligatorio");
        }
        this.dominioEmail = dominioEmail.trim();
        this.plantillas = Map.of(
                "TRANSFER:COMPLETED", this::transferenciaCompletada,
                "DEPOSIT:COMPLETED", this::recargaCompletada,
                "TRANSFER:REJECTED", this::transferenciaDenegada,
                "DEPOSIT:REJECTED", this::recargaDenegada);
    }

    public List<EmailCommandRecord> para(TransactionEventRecord evento) {
        return plantillas.getOrDefault(evento.clave(), e -> List.of()).apply(evento);
    }

    /** Formato exacto del enunciado: { "to": "user_a@email.com", "body": "Envió $50 a B" }. */
    private List<EmailCommandRecord> transferenciaCompletada(TransactionEventRecord e) {
        String monto = formatear(e.amount(), e.currency());
        return List.of(
                email(e.fromUser(), "Envió " + monto + " a " + e.toUser()),
                email(e.toUser(), "Recibió " + monto + " de " + e.fromUser()));
    }

    private List<EmailCommandRecord> recargaCompletada(TransactionEventRecord e) {
        return List.of(email(e.toUser(), "Recargó " + formatear(e.amount(), e.currency()) + " en su cuenta"
                + saldo(e.toBalance(), e.currency())));
    }

    private List<EmailCommandRecord> transferenciaDenegada(TransactionEventRecord e) {
        return List.of(email(e.fromUser(), "Su envío de " + formatear(e.amount(), e.currency()) + " a "
                + e.toUser() + " fue DENEGADO: " + motivo(e) + saldo(e.fromBalance(), e.currency())));
    }

    private List<EmailCommandRecord> recargaDenegada(TransactionEventRecord e) {
        return List.of(email(e.toUser(), "Su recarga de " + formatear(e.amount(), e.currency())
                + " fue DENEGADA: " + motivo(e)));
    }

    private EmailCommandRecord email(String usuario, String cuerpo) {
        return new EmailCommandRecord(direccionDe(usuario), cuerpo);
    }

    String direccionDe(String usuario) {
        return "user_" + usuario.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_") + "@" + dominioEmail;
    }

    private static String motivo(TransactionEventRecord e) {
        return Optional.ofNullable(e.reason()).map(r -> MOTIVOS.getOrDefault(r, r)).orElse("operación no permitida");
    }

    private static String saldo(BigDecimal saldo, String divisa) {
        return Optional.ofNullable(saldo).map(s -> ". Saldo disponible: " + formatear(s, divisa)).orElse("");
    }

    private static String formatear(BigDecimal monto, String divisa) {
        return SIMBOLOS.getOrDefault(divisa, divisa + " ") + monto.stripTrailingZeros().toPlainString();
    }
}
