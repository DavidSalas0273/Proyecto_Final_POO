package com.banco.ledger.fraud.domain.contract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FraudAlertRecordTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:31:04Z");

    @Test
    void seConstruyeDesdeElEvento() {
        TransactionEventRecord evento = new TransactionEventRecord("tx-9", "A", "B", new BigDecimal("20000"),
                "USD", "COMPLETED", null, AHORA);

        FraudAlertRecord alerta = FraudAlertRecord.de(evento, "HIGH_VALUE_TRANSACTION", AHORA);

        assertThat(alerta.txId()).isEqualTo("tx-9");
        assertThat(alerta.reason()).isEqualTo("HIGH_VALUE_TRANSACTION");
        assertThat(alerta.amount()).isEqualByComparingTo("20000");
    }

    @Test
    void validaCamposObligatorios() {
        assertThatThrownBy(() -> new FraudAlertRecord("", "R", "A", BigDecimal.ONE, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FraudAlertRecord("tx", null, "A", BigDecimal.ONE, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FraudAlertRecord("tx", "R", "A", BigDecimal.ONE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eventoRechazaMontoNoPositivo() {
        assertThatThrownBy(() -> new TransactionEventRecord("tx", "A", "B", BigDecimal.ZERO, "USD",
                "COMPLETED", null, AHORA)).isInstanceOf(IllegalArgumentException.class);
    }
}
