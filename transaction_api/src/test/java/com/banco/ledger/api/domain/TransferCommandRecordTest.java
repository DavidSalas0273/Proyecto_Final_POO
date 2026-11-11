package com.banco.ledger.api.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferCommandRecordTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:30:00Z");

    @Test
    void creaComandoValidoNormalizandoDivisa() {
        // Arrange & Act
        TransferCommandRecord comando = new TransferCommandRecord(
                "tx-1", " A ", "B", new BigDecimal("50"), "usd", AHORA);

        // Assert
        assertThat(comando.fromUser()).isEqualTo("A");
        assertThat(comando.currency()).isEqualTo("USD");
        assertThat(comando.amount()).isEqualByComparingTo("50");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-50.25"})
    void rechazaMontosMenoresOIgualesACero(String monto) {
        BigDecimal valor = new BigDecimal(monto);
        assertThatThrownBy(() -> new TransferCommandRecord("tx-1", "A", "B", valor, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void rechazaMontoNulo() {
        assertThatThrownBy(() -> new TransferCommandRecord("tx-1", "A", "B", null, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaIdentificadoresVacios(String valor) {
        assertThatThrownBy(() -> new TransferCommandRecord(valor, "A", "B", BigDecimal.TEN, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TransferCommandRecord("tx", valor, "B", BigDecimal.TEN, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TransferCommandRecord("tx", "A", valor, BigDecimal.TEN, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaDivisaNoAdmitida() {
        assertThatThrownBy(() -> new TransferCommandRecord("tx-1", "A", "B", BigDecimal.TEN, "BTC", AHORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Divisa no admitida");
    }

    @Test
    void rechazaTransferenciaAUnoMismo() {
        assertThatThrownBy(() -> new TransferCommandRecord("tx-1", "A", "a", BigDecimal.TEN, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
