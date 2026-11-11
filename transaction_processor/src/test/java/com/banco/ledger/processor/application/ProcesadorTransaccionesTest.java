package com.banco.ledger.processor.application;

import com.banco.ledger.processor.application.port.CuentaRepository;
import com.banco.ledger.processor.application.port.PublicadorEventosLedger;
import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.contract.TransferCommandRecord;
import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.PoliticaAperturaCuentas;
import com.banco.ledger.processor.domain.model.Transaccion;
import com.banco.ledger.processor.domain.regla.CadenaValidacion;
import com.banco.ledger.processor.domain.regla.DivisaCompatibleRegla;
import com.banco.ledger.processor.domain.regla.FondosSuficientesRegla;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesadorTransaccionesTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:30:01Z");

    @Mock
    private TransaccionRepository transacciones;
    @Mock
    private CuentaRepository cuentas;
    @Mock
    private PublicadorEventosLedger publicador;

    private ProcesadorTransacciones procesador;

    @BeforeEach
    void setUp() {
        CadenaValidacion validaciones = new CadenaValidacion(
                List.of(new DivisaCompatibleRegla(), new FondosSuficientesRegla()));
        PoliticaAperturaCuentas politica = new PoliticaAperturaCuentas(MontoDinero.de("100", "USD"));
        procesador = new ProcesadorTransacciones(transacciones, cuentas, publicador, validaciones, politica,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static TransferCommandRecord comando(String monto) {
        return new TransferCommandRecord("tx-1", "A", "B", new BigDecimal(monto), "USD",
                Instant.parse("2026-11-11T11:30:00Z"));
    }

    @Test
    void comandoValidoPersisteUnaVezYPublicaUnEventoCompleted() {
        // Arrange
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular("A")).thenReturn(Optional.of(
                CuentaBancaria.reconstituir("A", MontoDinero.de("1000", "USD"))));
        when(cuentas.buscarPorTitular("B")).thenReturn(Optional.empty());

        // Act
        Optional<TransactionEventRecord> resultado = procesador.procesar(comando("50"));

        // Assert
        verify(transacciones, times(1)).guardar(any(Transaccion.class));
        ArgumentCaptor<TransactionEventRecord> evento = ArgumentCaptor.forClass(TransactionEventRecord.class);
        verify(publicador, times(1)).publicar(evento.capture());
        assertThat(evento.getValue().status()).isEqualTo("COMPLETED");
        assertThat(evento.getValue().txId()).isEqualTo("tx-1");
        assertThat(resultado).contains(evento.getValue());

        ArgumentCaptor<CuentaBancaria> cuentasGuardadas = ArgumentCaptor.forClass(CuentaBancaria.class);
        verify(cuentas, times(2)).guardar(cuentasGuardadas.capture());
        assertThat(cuentasGuardadas.getAllValues())
                .extracting(c -> c.saldo().valor())
                .containsExactly(new BigDecimal("950.00"), new BigDecimal("150.00"));
    }

    @Test
    void sinFondosPublicaEventoRejectedConMotivo() {
        // Arrange
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular(any())).thenReturn(Optional.empty());

        // Act
        procesador.procesar(comando("500"));

        // Assert
        ArgumentCaptor<TransactionEventRecord> evento = ArgumentCaptor.forClass(TransactionEventRecord.class);
        verify(transacciones, times(1)).guardar(any(Transaccion.class));
        verify(publicador, times(1)).publicar(evento.capture());
        assertThat(evento.getValue().status()).isEqualTo("REJECTED");
        assertThat(evento.getValue().reason()).isEqualTo(Transaccion.MOTIVO_FONDOS_INSUFICIENTES);
    }

    @Test
    void comandoDuplicadoNoSeReprocesa() {
        // Arrange
        when(transacciones.existe(any())).thenReturn(true);

        // Act
        Optional<TransactionEventRecord> resultado = procesador.procesar(comando("50"));

        // Assert
        assertThat(resultado).isEmpty();
        verify(transacciones, never()).guardar(any());
        verify(publicador, never()).publicar(any());
    }
}
