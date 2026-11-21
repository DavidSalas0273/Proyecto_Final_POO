package com.banco.ledger.processor.service;

import com.banco.ledger.processor.dto.DepositCommandRecord;
import com.banco.ledger.processor.dto.TransactionEventRecord;
import com.banco.ledger.processor.dto.TransferCommandRecord;
import com.banco.ledger.processor.kafka.PublicadorEventosLedger;
import com.banco.ledger.processor.model.CuentaBancaria;
import com.banco.ledger.processor.model.MontoDinero;
import com.banco.ledger.processor.model.PoliticaAperturaCuentas;
import com.banco.ledger.processor.model.Transaccion;
import com.banco.ledger.processor.regla.CadenaValidacion;
import com.banco.ledger.processor.regla.DivisaCompatibleRegla;
import com.banco.ledger.processor.regla.FondosSuficientesRegla;
import com.banco.ledger.processor.repository.CuentaRepository;
import com.banco.ledger.processor.repository.TransaccionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private Clock clock;

    @Spy
    private CadenaValidacion validaciones = new CadenaValidacion(
            List.of(new DivisaCompatibleRegla(), new FondosSuficientesRegla()));
    @Spy
    private PoliticaAperturaCuentas politica = new PoliticaAperturaCuentas(MontoDinero.cero("USD"));

    @InjectMocks
    private ProcesadorTransacciones procesador;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(AHORA);
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
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular("A")).thenReturn(Optional.of(
                CuentaBancaria.reconstituir("A", MontoDinero.de("1000", "USD"))));
        when(cuentas.buscarPorTitular("B")).thenReturn(Optional.empty());

        Optional<TransactionEventRecord> resultado = procesador.procesar(transferencia("50"));

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
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular(any())).thenReturn(Optional.empty());

        procesador.procesar(transferencia("500"));

        verify(transacciones, times(1)).guardar(any(TransactionEventRecord.class));
        TransactionEventRecord evento = eventoPublicado();
        assertThat(evento.status()).isEqualTo("REJECTED");
        assertThat(evento.reason()).isEqualTo(Transaccion.MOTIVO_FONDOS_INSUFICIENTES);
        assertThat(evento.fromBalance()).isEqualByComparingTo("0");
        assertThat(evento.toBalance()).isEqualByComparingTo("0");
    }

    @Test
    void recargaSumaAlSaldoDelTitular() {
        when(transacciones.existe(any())).thenReturn(false);
        when(cuentas.buscarPorTitular("A")).thenReturn(Optional.of(
                CuentaBancaria.reconstituir("A", MontoDinero.de("20", "USD"))));

        procesador.procesar(new DepositCommandRecord("tx-r", "A", new BigDecimal("80"), "USD", SOLICITADA));

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
        when(transacciones.existe(any())).thenReturn(true);

        Optional<TransactionEventRecord> resultado = procesador.procesar(transferencia("50"));

        assertThat(resultado).isEmpty();
        verify(transacciones, never()).guardar(any());
        verify(publicador, never()).publicar(any());
    }
}
