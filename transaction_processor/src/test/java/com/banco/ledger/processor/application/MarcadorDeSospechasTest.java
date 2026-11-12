package com.banco.ledger.processor.application;

import com.banco.ledger.processor.application.port.PublicadorEventosLedger;
import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.FraudAlertRecord;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.estado.CatalogoEstados;
import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.TipoMovimiento;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarcadorDeSospechasTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:31:04Z");
    private static final FraudAlertRecord ALERTA = new FraudAlertRecord("tx-9", "HIGH_VALUE_TRANSACTION");

    @Mock
    private TransaccionRepository transacciones;
    @Mock
    private PublicadorEventosLedger publicador;
    @Mock
    private Clock clock;
    @InjectMocks
    private MarcadorDeSospechas marcador;

    private static Transaccion registrada(String estado) {
        return Transaccion.reconstituir(new TransactionId("tx-9"), TipoMovimiento.TRANSFER, "C", "D",
                MontoDinero.de("20000", "USD"), CatalogoEstados.desdeCodigo(estado), null,
                Instant.parse("2026-11-11T11:31:02Z"));
    }

    @Test
    void transaccionCompletadaPasaASospechosaSeAuditaYSePublica() {
        // Arrange
        when(transacciones.buscar(new TransactionId("tx-9"))).thenReturn(Optional.of(registrada("COMPLETED")));
        when(clock.instant()).thenReturn(AHORA);

        // Act
        Optional<TransactionEventRecord> resultado = marcador.marcar(ALERTA);

        // Assert
        ArgumentCaptor<TransactionEventRecord> evento = ArgumentCaptor.forClass(TransactionEventRecord.class);
        verify(transacciones, times(1)).registrarCambioDeEstado(evento.capture());
        verify(publicador, times(1)).publicar(evento.getValue());
        assertThat(evento.getValue().status()).isEqualTo("SUSPICIOUS");
        assertThat(evento.getValue().reason()).isEqualTo("HIGH_VALUE_TRANSACTION");
        assertThat(evento.getValue().timestamp()).isEqualTo(AHORA);
        assertThat(resultado).contains(evento.getValue());
    }

    @Test
    void alertaRepetidaNoVuelveAMarcarNiPublicar() {
        when(transacciones.buscar(any())).thenReturn(Optional.of(registrada("SUSPICIOUS")));

        assertThat(marcador.marcar(ALERTA)).isEmpty();
        verify(transacciones, never()).registrarCambioDeEstado(any());
        verify(publicador, never()).publicar(any());
    }

    @Test
    void transaccionRechazadaNoPuedeSerSospechosa() {
        when(transacciones.buscar(any())).thenReturn(Optional.of(registrada("REJECTED")));

        assertThat(marcador.marcar(ALERTA)).isEmpty();
        verify(publicador, never()).publicar(any());
    }

    @Test
    void siLaTransaccionAunNoExisteFallaParaQueKafkaReintente() {
        when(transacciones.buscar(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marcador.marcar(ALERTA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tx-9");
        verify(publicador, never()).publicar(any());
    }
}
