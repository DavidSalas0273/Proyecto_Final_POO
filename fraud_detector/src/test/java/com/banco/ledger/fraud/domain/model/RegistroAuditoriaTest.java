package com.banco.ledger.fraud.domain.model;

import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;
import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroAuditoriaTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:31:04Z");

    private static TransactionEventRecord evento(String tipo, String estado) {
        return new TransactionEventRecord("tx-9", "C", "D", new BigDecimal("20000"), "USD", estado, null,
                Instant.parse("2026-11-11T11:31:02Z"), tipo);
    }

    @Test
    void abreElRegistroYProduceLaAlertaPublica() {
        // Arrange & Act
        RegistroAuditoria registro = RegistroAuditoria.abrirPorSospecha(
                evento("TRANSFER", "COMPLETED"), " HIGH_VALUE_TRANSACTION ", AHORA);

        // Assert
        FraudAlertRecord alerta = registro.aAlerta();
        assertThat(alerta.txId()).isEqualTo("tx-9");
        assertThat(alerta.reason()).isEqualTo("HIGH_VALUE_TRANSACTION");
        assertThat(alerta.fromUser()).isEqualTo("C");
        assertThat(alerta.amount()).isEqualByComparingTo("20000");
        assertThat(alerta.detectedAt()).isEqualTo(AHORA);
    }

    @Test
    void noSePuedeAuditarUnaTransaccionNoCompletada() {
        TransactionEventRecord rechazada = evento("TRANSFER", "REJECTED");

        assertThatThrownBy(() -> RegistroAuditoria.abrirPorSospecha(rechazada, "HIGH_VALUE_TRANSACTION", AHORA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    void noSePuedeAuditarUnaRecarga() {
        TransactionEventRecord recarga = new TransactionEventRecord("tx-r", null, "A", new BigDecimal("50000"),
                "USD", "COMPLETED", null, AHORA, "DEPOSIT");

        assertThatThrownBy(() -> RegistroAuditoria.abrirPorSospecha(recarga, "HIGH_VALUE_TRANSACTION", AHORA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exigeMotivoYFecha() {
        TransactionEventRecord completada = evento("TRANSFER", "COMPLETED");

        assertThatThrownBy(() -> RegistroAuditoria.abrirPorSospecha(completada, " ", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RegistroAuditoria.abrirPorSospecha(completada, "HIGH_VALUE_TRANSACTION", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
