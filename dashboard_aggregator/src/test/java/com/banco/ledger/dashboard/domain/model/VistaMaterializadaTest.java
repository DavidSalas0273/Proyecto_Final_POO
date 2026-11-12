package com.banco.ledger.dashboard.domain.model;

import com.banco.ledger.dashboard.domain.contract.FraudAlertRecord;
import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VistaMaterializadaTest {

    private static final Instant T0 = Instant.parse("2026-11-11T11:31:02Z");

    private static TransactionEventRecord evento(String txId, String monto) {
        return new TransactionEventRecord(txId, "A", "B", new BigDecimal(monto), "USD", "COMPLETED", null,
                T0, "TRANSFER", null, null);
    }

    private static TransactionEventRecord conSaldos(String txId, String tipo, String from, String status,
                                                    String fromBalance, String toBalance, Instant momento) {
        return new TransactionEventRecord(txId, from, "B", BigDecimal.TEN, "USD", status, null, momento, tipo,
                fromBalance == null ? null : new BigDecimal(fromBalance), new BigDecimal(toBalance));
    }

    @Test
    void transaccionNuevaApareceSinMarcaDeFraude() {
        // Arrange
        VistaMaterializada vista = new VistaMaterializada(10);

        // Act
        TransaccionVista fila = vista.aplicarTransaccion(evento("tx-1", "50"));

        // Assert
        assertThat(fila.suspicious()).isFalse();
        assertThat(vista.instantanea()).containsExactly(fila);
    }

    @Test
    void alertaPosteriorMarcaLaTransaccionComoSospechosa() {
        VistaMaterializada vista = new VistaMaterializada(10);
        vista.aplicarTransaccion(evento("tx-1", "20000"));

        assertThat(vista.aplicarAlerta(new FraudAlertRecord("tx-1", "HIGH_VALUE_TRANSACTION")))
                .hasValueSatisfying(fila -> {
                    assertThat(fila.suspicious()).isTrue();
                    assertThat(fila.fraudReason()).isEqualTo("HIGH_VALUE_TRANSACTION");
                });
    }

    @Test
    void alertaQueLlegaAntesSeAplicaCuandoLlegaLaTransaccion() {
        VistaMaterializada vista = new VistaMaterializada(10);

        assertThat(vista.aplicarAlerta(new FraudAlertRecord("tx-1", "HIGH_VALUE_TRANSACTION"))).isEmpty();
        TransaccionVista fila = vista.aplicarTransaccion(evento("tx-1", "20000"));

        assertThat(fila.suspicious()).isTrue();
    }

    @Test
    void reprocesarElEventoNoBorraLaMarcaDeSospecha() {
        VistaMaterializada vista = new VistaMaterializada(10);
        vista.aplicarTransaccion(evento("tx-1", "20000"));
        vista.aplicarAlerta(new FraudAlertRecord("tx-1", "HIGH_VALUE_TRANSACTION"));

        assertThat(vista.aplicarTransaccion(evento("tx-1", "20000")).suspicious()).isTrue();
    }

    @Test
    void instantaneaOrdenaRecientesPrimeroYRespetaCapacidad() {
        VistaMaterializada vista = new VistaMaterializada(2);
        vista.aplicarTransaccion(evento("tx-1", "1"));
        vista.aplicarTransaccion(evento("tx-2", "2"));
        vista.aplicarTransaccion(evento("tx-3", "3"));

        assertThat(vista.instantanea()).extracting(TransaccionVista::txId).containsExactly("tx-3", "tx-2");
    }

    @Test
    void losEventosActualizanLosSaldosDeLasCuentas() {
        // Arrange
        VistaMaterializada vista = new VistaMaterializada(10);

        // Act: recarga a B y luego transferencia denegada de A a B
        vista.aplicarTransaccion(conSaldos("tx-r", "DEPOSIT", null, "COMPLETED", null, "100", T0));
        vista.aplicarTransaccion(conSaldos("tx-t", "TRANSFER", "A", "REJECTED", "0", "100", T0.plusSeconds(5)));

        // Assert
        assertThat(vista.cuentas())
                .extracting(SaldoCuenta::usuario, c -> c.saldo().intValue())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("A", 0),
                        org.assertj.core.groups.Tuple.tuple("B", 100));
    }

    @Test
    void unEventoAntiguoNoSobrescribeUnSaldoMasReciente() {
        VistaMaterializada vista = new VistaMaterializada(10);
        vista.aplicarTransaccion(conSaldos("tx-2", "DEPOSIT", null, "COMPLETED", null, "300", T0.plusSeconds(10)));
        vista.aplicarTransaccion(conSaldos("tx-1", "DEPOSIT", null, "COMPLETED", null, "100", T0));

        assertThat(vista.cuentas()).singleElement()
                .satisfies(c -> assertThat(c.saldo()).isEqualByComparingTo("300"));
    }

    @Test
    void eventoSuspiciousDelLedgerMarcaLaFilaYConservaLosSaldos() {
        // Arrange: transferencia completada con saldos
        VistaMaterializada vista = new VistaMaterializada(10);
        vista.aplicarTransaccion(conSaldos("tx-9", "TRANSFER", "C", "COMPLETED", "5000", "20000", T0));

        // Act: el procesador publica el cambio de estado (sin saldos)
        TransaccionVista fila = vista.aplicarTransaccion(new TransactionEventRecord("tx-9", "C", "B",
                BigDecimal.TEN, "USD", "SUSPICIOUS", "HIGH_VALUE_TRANSACTION", T0.plusSeconds(2), "TRANSFER",
                null, null));

        // Assert
        assertThat(fila.suspicious()).isTrue();
        assertThat(fila.fraudReason()).isEqualTo("HIGH_VALUE_TRANSACTION");
        assertThat(vista.cuentas()).extracting(c -> c.saldo().intValue()).containsExactly(20000, 5000);
    }

    @Test
    void capacidadInvalidaFallaRapido() {
        assertThatThrownBy(() -> new VistaMaterializada(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
