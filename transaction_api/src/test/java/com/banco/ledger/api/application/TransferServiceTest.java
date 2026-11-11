package com.banco.ledger.api.application;

import com.banco.ledger.api.application.port.TransferCommandPublisher;
import com.banco.ledger.api.application.port.TxIdGenerator;
import com.banco.ledger.api.domain.DepositCommandRecord;
import com.banco.ledger.api.domain.TransferCommandRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:30:00Z");

    @Mock
    private TransferCommandPublisher publisher;
    @Mock
    private TxIdGenerator txIdGenerator;

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService(publisher, txIdGenerator, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void publicaExactamenteUnComandoConElTxIdGenerado() {
        // Arrange
        when(txIdGenerator.nuevoId()).thenReturn("tx-123");

        // Act
        TransferCommandRecord resultado = service.solicitarTransferencia("A", "B", new BigDecimal("50"), "USD");

        // Assert
        ArgumentCaptor<TransferCommandRecord> captor = ArgumentCaptor.forClass(TransferCommandRecord.class);
        verify(publisher, times(1)).publicar(captor.capture());
        assertThat(captor.getValue()).isEqualTo(resultado);
        assertThat(resultado.txId()).isEqualTo("tx-123");
        assertThat(resultado.requestedAt()).isEqualTo(AHORA);
    }

    @Test
    void recargaPublicaExactamenteUnComandoDeposit() {
        // Arrange
        when(txIdGenerator.nuevoId()).thenReturn("tx-r");

        // Act
        DepositCommandRecord recarga = service.solicitarRecarga("A", new BigDecimal("100"), "USD");

        // Assert
        verify(publisher, times(1)).publicar(recarga);
        assertThat(recarga.toUser()).isEqualTo("A");
        assertThat(recarga.txId()).isEqualTo("tx-r");
    }

    @Test
    void noPublicaNadaSiElComandoEsInvalido() {
        // Arrange
        when(txIdGenerator.nuevoId()).thenReturn("tx-123");
        BigDecimal montoNegativo = new BigDecimal("-5");

        // Act & Assert
        assertThatThrownBy(() -> service.solicitarTransferencia("A", "B", montoNegativo, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(publisher, never()).publicar(any());
    }
}
