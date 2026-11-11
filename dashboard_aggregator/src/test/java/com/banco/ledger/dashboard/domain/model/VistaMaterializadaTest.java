package com.banco.ledger.dashboard.domain.model;

import com.banco.ledger.dashboard.domain.contract.FraudAlertRecord;
import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VistaMaterializadaTest {

    private static TransactionEventRecord evento(String txId, String monto) {
        return new TransactionEventRecord(txId, "A", "B", new BigDecimal(monto), "USD", "COMPLETED", null,
                Instant.parse("2026-11-11T11:31:02Z"));
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
    void capacidadInvalidaFallaRapido() {
        assertThatThrownBy(() -> new VistaMaterializada(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
