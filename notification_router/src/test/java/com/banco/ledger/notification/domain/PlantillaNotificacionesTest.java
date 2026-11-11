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

    private final PlantillaNotificaciones plantilla = new PlantillaNotificaciones("email.com");

    @Test
    void generaEmailsParaEmisorYReceptor() {
        // Arrange
        TransactionEventRecord evento = new TransactionEventRecord("tx-1", "A", "B", new BigDecimal("50.00"),
                "USD", "COMPLETED", null, Instant.now());

        // Act
        List<EmailCommandRecord> emails = plantilla.paraTransferencia(evento);

        // Assert
        assertThat(emails).containsExactly(
                new EmailCommandRecord("user_a@email.com", "Envió $50 a B"),
                new EmailCommandRecord("user_b@email.com", "Recibió $50 de A"));
    }

    @Test
    void emailCommandValidaDestinatarioYCuerpo() {
        assertThatThrownBy(() -> new EmailCommandRecord("sin-arroba", "hola"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmailCommandRecord("a@b.com", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
