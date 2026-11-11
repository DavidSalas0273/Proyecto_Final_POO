package com.banco.ledger.notification.application;

import com.banco.ledger.notification.application.port.PublicadorComandosEmail;
import com.banco.ledger.notification.domain.PlantillaNotificaciones;
import com.banco.ledger.notification.domain.contract.EmailCommandRecord;
import com.banco.ledger.notification.domain.contract.TransactionEventRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnrutadorNotificacionesTest {

    @Spy
    private PlantillaNotificaciones plantilla = new PlantillaNotificaciones("email.com");
    @Mock
    private PublicadorComandosEmail publicador;
    @InjectMocks
    private EnrutadorNotificaciones enrutador;

    private static TransactionEventRecord evento(String estado) {
        return new TransactionEventRecord("tx-1", "A", "B", new BigDecimal("50"), "USD", estado, null,
                Instant.parse("2026-11-11T11:30:02Z"));
    }

    @Test
    void eventoCompletadoPublicaDosComandosDeEmail() {
        // Act
        enrutador.enrutar(evento("COMPLETED"));

        // Assert
        verify(publicador, times(1)).publicar(new EmailCommandRecord("user_a@email.com", "Envió $50 a B"));
        verify(publicador, times(1)).publicar(new EmailCommandRecord("user_b@email.com", "Recibió $50 de A"));
        verify(publicador, times(2)).publicar(any());
    }

    @Test
    void eventoRechazadoNoGeneraNotificaciones() {
        enrutador.enrutar(evento("REJECTED"));

        verifyNoInteractions(publicador);
    }
}
