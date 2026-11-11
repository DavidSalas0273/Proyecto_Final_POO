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

    private static TransactionEventRecord evento(String estado, String motivo) {
        return new TransactionEventRecord("tx-1", "A", "B", new BigDecimal("50"), "USD", estado, motivo,
                Instant.parse("2026-11-11T11:30:02Z"), "TRANSFER", null, null);
    }

    @Test
    void eventoCompletadoPublicaDosComandosDeEmail() {
        // Act
        enrutador.enrutar(evento("COMPLETED", null));

        // Assert
        verify(publicador, times(1)).publicar(new EmailCommandRecord("user_a@email.com", "Envió $50 a B"));
        verify(publicador, times(1)).publicar(new EmailCommandRecord("user_b@email.com", "Recibió $50 de A"));
        verify(publicador, times(2)).publicar(any());
    }

    @Test
    void eventoDenegadoPublicaUnAvisoAlEmisor() {
        enrutador.enrutar(evento("REJECTED", "INSUFFICIENT_FUNDS"));

        verify(publicador, times(1)).publicar(any());
    }

    @Test
    void eventoSinPlantillaNoGeneraNotificaciones() {
        enrutador.enrutar(evento("SUSPICIOUS", null));

        verifyNoInteractions(publicador);
    }
}
