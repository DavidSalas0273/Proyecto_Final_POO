package com.banco.ledger.api.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepositCommandRecordTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:30:00Z");

    @Test
    void creaRecargaValida() {
        DepositCommandRecord recarga = new DepositCommandRecord("tx-r", " A ", new BigDecimal("100"), "usd", AHORA);

        assertThat(recarga.toUser()).isEqualTo("A");
        assertThat(recarga.currency()).isEqualTo("USD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10"})
    void rechazaMontosNoPositivos(String monto) {
        BigDecimal valor = new BigDecimal(monto);
        assertThatThrownBy(() -> new DepositCommandRecord("tx", "A", valor, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUsuarioVacioYDivisaNoAdmitida() {
        assertThatThrownBy(() -> new DepositCommandRecord("tx", " ", BigDecimal.TEN, "USD", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DepositCommandRecord("tx", "A", BigDecimal.TEN, "BTC", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elJsonIncluyeElTipoDeComando() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        String recarga = mapper.writeValueAsString(
                new DepositCommandRecord("tx-r", "A", BigDecimal.TEN, "USD", AHORA));
        String transferencia = mapper.writeValueAsString(
                new TransferCommandRecord("tx-t", "A", "B", BigDecimal.TEN, "USD", AHORA));

        assertThat(recarga).contains("\"type\":\"DEPOSIT\"").contains("\"to_user\":\"A\"");
        assertThat(transferencia).contains("\"type\":\"TRANSFER\"");
    }
}
