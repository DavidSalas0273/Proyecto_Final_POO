package com.banco.ledger.processor.domain.model;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransaccionTest {

    private static final Instant T0 = Instant.parse("2026-11-11T11:30:00Z");
    private static final Instant T1 = Instant.parse("2026-11-11T11:30:01Z");

    private CuentaBancaria cuentaA;
    private CuentaBancaria cuentaB;

    @BeforeEach
    void setUp() {
        cuentaA = CuentaBancaria.abrir("A", MontoDinero.de("100", "USD"));
        cuentaB = CuentaBancaria.abrir("B", MontoDinero.de("0", "USD"));
    }

    private Transaccion nuevaTransferencia(String monto) {
        return Transaccion.recibirTransferencia(new TransactionId("tx-1"), "A", "B", MontoDinero.de(monto, "USD"), T0);
    }

    @Test
    void nuevaTransaccionEstaRecibida() {
        assertThat(nuevaTransferencia("50").estadoActual()).isEqualTo("RECEIVED");
        assertThat(nuevaTransferencia("50").tipo()).isEqualTo(TipoMovimiento.TRANSFER);
    }

    @Test
    void flujoNormalRecibidaProcesandoCompletada() {
        // Arrange
        Transaccion tx = nuevaTransferencia("50");

        // Act
        tx.iniciarProcesamiento(T0);
        tx.ejecutarTransferencia(cuentaA, cuentaB, T1);

        // Assert
        assertThat(tx.estadoActual()).isEqualTo("COMPLETED");
        assertThat(cuentaA.saldo()).isEqualTo(MontoDinero.de("50", "USD"));
        assertThat(cuentaB.saldo()).isEqualTo(MontoDinero.de("50", "USD"));
    }

    @Test
    void recargaSoloAcreditaAlTitular() {
        // Arrange
        Transaccion recarga = Transaccion.recibirRecarga(new TransactionId("tx-r"), "B",
                MontoDinero.de("250", "USD"), T0);
        recarga.iniciarProcesamiento(T0);

        // Act
        recarga.ejecutarRecarga(cuentaB, T1);

        // Assert
        assertThat(recarga.tipo()).isEqualTo(TipoMovimiento.DEPOSIT);
        assertThat(recarga.origen()).isEmpty();
        assertThat(recarga.estadoActual()).isEqualTo("COMPLETED");
        assertThat(cuentaB.saldo()).isEqualTo(MontoDinero.de("250", "USD"));
    }

    @Test
    void recargaNoAceptaCuentaDeOrigen() {
        Transaccion recarga = Transaccion.recibirRecarga(new TransactionId("tx-r"), "B",
                MontoDinero.de("10", "USD"), T0);
        recarga.iniciarProcesamiento(T0);

        assertThatThrownBy(() -> recarga.ejecutar(Optional.of(cuentaA), cuentaB, T1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cuentaB.saldo()).isEqualTo(MontoDinero.cero("USD"));
    }

    @Test
    void transferenciaSinCuentaDeOrigenFallaSinTocarSaldos() {
        Transaccion tx = nuevaTransferencia("10");
        tx.iniciarProcesamiento(T0);

        assertThatThrownBy(() -> tx.ejecutar(Optional.empty(), cuentaB, T1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cuentaB.saldo()).isEqualTo(MontoDinero.cero("USD"));
    }

    @Test
    void noSePuedeEjecutarSinIniciarProcesamientoYNoTocaSaldos() {
        Transaccion tx = nuevaTransferencia("50");

        assertThatThrownBy(() -> tx.ejecutarTransferencia(cuentaA, cuentaB, T1))
                .isInstanceOf(IllegalStateException.class);
        assertThat(cuentaA.saldo()).isEqualTo(MontoDinero.de("100", "USD"));
        assertThat(cuentaB.saldo()).isEqualTo(MontoDinero.de("0", "USD"));
    }

    @Test
    void rechazoPorFondosInsuficientesEsEstadoFinal() {
        // Arrange
        Transaccion tx = nuevaTransferencia("500");
        tx.iniciarProcesamiento(T0);

        // Act
        tx.rechazarPorFondosInsuficientes(T1);

        // Assert
        assertThat(tx.estadoActual()).isEqualTo("REJECTED");
        assertThat(tx.motivo()).contains(Transaccion.MOTIVO_FONDOS_INSUFICIENTES);
        assertThatThrownBy(() -> tx.ejecutarTransferencia(cuentaA, cuentaB, T1))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tx.marcarComoSospechosa("HIGH_VALUE", T1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void soloUnaTransaccionCompletadaPuedeMarcarseComoSospechosa() {
        Transaccion pendiente = nuevaTransferencia("50");
        assertThatThrownBy(() -> pendiente.marcarComoSospechosa("HIGH_VALUE", T1))
                .isInstanceOf(IllegalStateException.class);

        Transaccion completada = nuevaTransferencia("50");
        completada.iniciarProcesamiento(T0);
        completada.ejecutarTransferencia(cuentaA, cuentaB, T1);
        completada.marcarComoSospechosa("HIGH_VALUE", T1);

        assertThat(completada.estadoActual()).isEqualTo("SUSPICIOUS");
    }

    @Test
    void rechazaCuentaQueNoPerteneceAlOrigen() {
        Transaccion tx = nuevaTransferencia("10");
        tx.iniciarProcesamiento(T0);

        assertThatThrownBy(() -> tx.ejecutarTransferencia(cuentaB, cuentaA, T1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invariantesDeCreacion() {
        TransactionId id = new TransactionId("tx");
        MontoDinero cero = MontoDinero.cero("USD");
        MontoDinero diez = MontoDinero.de("10", "USD");

        assertThatThrownBy(() -> Transaccion.recibirTransferencia(id, "A", "B", cero, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transaccion.recibirTransferencia(id, "A", "A", diez, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transaccion.recibirTransferencia(id, "", "B", diez, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transaccion.recibirRecarga(id, " ", diez, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transaccion.recibirRecarga(id, "A", cero, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void proyectaElEventoDelLedgerConSaldos() {
        Transaccion tx = nuevaTransferencia("50");
        tx.iniciarProcesamiento(T0);
        tx.ejecutarTransferencia(cuentaA, cuentaB, T1);

        TransactionEventRecord evento = tx.aEvento(Optional.of(cuentaA.saldo()), cuentaB.saldo());

        assertThat(evento.txId()).isEqualTo("tx-1");
        assertThat(evento.type()).isEqualTo("TRANSFER");
        assertThat(evento.status()).isEqualTo("COMPLETED");
        assertThat(evento.amount()).isEqualByComparingTo("50");
        assertThat(evento.fromBalance()).isEqualByComparingTo("50");
        assertThat(evento.toBalance()).isEqualByComparingTo("50");
        assertThat(evento.reason()).isNull();
        assertThat(evento.timestamp()).isEqualTo(T1);
    }

    @Test
    void eventoDeRecargaNoTieneOrigen() {
        Transaccion recarga = Transaccion.recibirRecarga(new TransactionId("tx-r"), "B",
                MontoDinero.de("30", "USD"), T0);
        recarga.iniciarProcesamiento(T0);
        recarga.ejecutarRecarga(cuentaB, T1);

        TransactionEventRecord evento = recarga.aEvento(Optional.empty(), cuentaB.saldo());

        assertThat(evento.type()).isEqualTo("DEPOSIT");
        assertThat(evento.fromUser()).isNull();
        assertThat(evento.fromBalance()).isNull();
        assertThat(evento.toBalance()).isEqualByComparingTo("30");
    }
}
