package com.banco.ledger.processor.application;

import com.banco.ledger.processor.application.port.CuentaRepository;
import com.banco.ledger.processor.application.port.PublicadorEventosLedger;
import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.DepositCommandRecord;
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
    private static final Instant SOLICITADA = Instant.parse("2026-11-11T11:30:00Z");

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
        PoliticaAperturaCuentas politica = new PoliticaAperturaCuentas(MontoDinero.cero("USD"));
        procesador = new ProcesadorTransacciones(transacciones, cuentas, publicador, validaciones, politica,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static TransferCommandRecord transferencia(String monto) {
        return new TransferCommandRecord("tx-1", "A", "B", new BigDecimal(monto), "USD", SOLICITADA);
    }

    private TransactionEventRecord eventoPublicado() {
        ArgumentCaptor<TransactionEventRecord> evento = ArgumentCaptor.forClass(TransactionEventRecord.class);
        verify(publicador, times(1)).publicar(evento.capture());
        return evento.getValue();
    }

    @Test
    void transferenciaConSaldoPersisteUnaVezYPublicaEventoCompletedConSaldos() {
        // Arrange
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular("A")).thenReturn(Optional.of(
                CuentaBancaria.reconstituir("A", MontoDinero.de("1000", "USD"))));
        when(cuentas.buscarPorTitular("B")).thenReturn(Optional.empty());

        // Act
        Optional<TransactionEventRecord> resultado = procesador.procesar(transferencia("50"));

        // Assert
        verify(transacciones, times(1)).guardar(any(TransactionEventRecord.class));
        TransactionEventRecord evento = eventoPublicado();
        assertThat(evento.status()).isEqualTo("COMPLETED");
        assertThat(evento.type()).isEqualTo("TRANSFER");
        assertThat(evento.fromBalance()).isEqualByComparingTo("950");
        assertThat(evento.toBalance()).isEqualByComparingTo("50");
        assertThat(resultado).contains(evento);

        ArgumentCaptor<CuentaBancaria> cuentasGuardadas = ArgumentCaptor.forClass(CuentaBancaria.class);
        verify(cuentas, times(2)).guardar(cuentasGuardadas.capture());
        assertThat(cuentasGuardadas.getAllValues())
                .extracting(c -> c.saldo().valor())
                .containsExactly(new BigDecimal("950.00"), new BigDecimal("50.00"));
    }

    @Test
    void transferenciaSinSaldoSeDeniegaYNoMueveDinero() {
        // Arrange: cuentas nuevas empiezan en 0
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular(any())).thenReturn(Optional.empty());

        // Act
        procesador.procesar(transferencia("500"));

        // Assert
        verify(transacciones, times(1)).guardar(any(TransactionEventRecord.class));
        TransactionEventRecord evento = eventoPublicado();
        assertThat(evento.status()).isEqualTo("REJECTED");
        assertThat(evento.reason()).isEqualTo(Transaccion.MOTIVO_FONDOS_INSUFICIENTES);
        assertThat(evento.fromBalance()).isEqualByComparingTo("0");
        assertThat(evento.toBalance()).isEqualByComparingTo("0");
    }

    @Test
    void recargaSumaAlSaldoDelTitular() {
        // Arrange
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular("A")).thenReturn(Optional.of(
                CuentaBancaria.reconstituir("A", MontoDinero.de("20", "USD"))));

        // Act
        procesador.procesar(new DepositCommandRecord("tx-r", "A", new BigDecimal("80"), "USD", SOLICITADA));

        // Assert
        TransactionEventRecord evento = eventoPublicado();
        assertThat(evento.type()).isEqualTo("DEPOSIT");
        assertThat(evento.status()).isEqualTo("COMPLETED");
        assertThat(evento.fromUser()).isNull();
        assertThat(evento.toBalance()).isEqualByComparingTo("100");
        verify(cuentas, times(1)).guardar(any(CuentaBancaria.class));
        verify(transacciones, times(1)).guardar(any(TransactionEventRecord.class));
    }

    @Test
    void comandoDuplicadoNoSeReprocesa() {
        // Arrange
        when(transacciones.existe(any())).thenReturn(true);

        // Act
        Optional<TransactionEventRecord> resultado = procesador.procesar(transferencia("50"));

        // Assert
        assertThat(resultado).isEmpty();
        verify(transacciones, never()).guardar(any());
        verify(publicador, never()).publicar(any());
    }
}
