package com.banco.ledger.notification.domain;

import com.banco.ledger.notification.domain.contract.EmailCommandRecord;
import com.banco.ledger.notification.domain.contract.TransactionEventRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlantillaNotificacionesTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:30:02Z");

    private final PlantillaNotificaciones plantilla = new PlantillaNotificaciones("email.com");

    private static TransactionEventRecord evento(String tipo, String from, String status, String reason,
                                                 String fromBalance, String toBalance) {
        return new TransactionEventRecord("tx-1", from, "B", new BigDecimal("50.00"), "USD", status, reason, AHORA,
                tipo, fromBalance == null ? null : new BigDecimal(fromBalance),
                toBalance == null ? null : new BigDecimal(toBalance));
    }

    @Test
    void transferenciaCompletadaUsaElFormatoExactoDelEnunciado() {
        // Act
        List<EmailCommandRecord> emails = plantilla.para(evento("TRANSFER", "A", "COMPLETED", null, "50", "150"));

        // Assert
        assertThat(emails).containsExactly(
                new EmailCommandRecord("user_a@email.com", "Envió $50 a B"),
                new EmailCommandRecord("user_b@email.com", "Recibió $50 de A"));
    }

    @Test
    void eventoAntiguoSinTipoSeTrataComoTransferencia() {
        List<EmailCommandRecord> emails = plantilla.para(evento(null, "A", "COMPLETED", null, null, null));

        assertThat(emails).first().isEqualTo(new EmailCommandRecord("user_a@email.com", "Envió $50 a B"));
    }

    @Test
    void recargaCompletadaNotificaAlTitular() {
        List<EmailCommandRecord> emails = plantilla.para(evento("DEPOSIT", null, "COMPLETED", null, null, "250"));

        assertThat(emails).containsExactly(
                new EmailCommandRecord("user_b@email.com", "Recargó $50 en su cuenta. Saldo disponible: $250"));
    }

    @Test
    void transferenciaDenegadaAvisaAlEmisorConElMotivo() {
        List<EmailCommandRecord> emails =
                plantilla.para(evento("TRANSFER", "A", "REJECTED", "INSUFFICIENT_FUNDS", "10", "0"));

        assertThat(emails).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("user_a@email.com");
            assertThat(email.body()).contains("DENEGADO").contains("fondos insuficientes");
        });
    }

    @Test
    void estadosSinPlantillaNoGeneranEmails() {
        assertThat(plantilla.para(evento("TRANSFER", "A", "SUSPICIOUS", null, null, null))).isEmpty();
    }

    @Test
    void emailCommandValidaDestinatarioYCuerpo() {
        assertThatThrownBy(() -> new EmailCommandRecord("sin-arroba", "hola"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmailCommandRecord("a@b.com", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
