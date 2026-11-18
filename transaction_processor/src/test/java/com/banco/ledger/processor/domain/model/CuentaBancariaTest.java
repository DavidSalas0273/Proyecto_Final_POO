package com.banco.ledger.processor.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaBancariaTest {
    @Test
    void debitarReduceElSaldo() {
        CuentaBancaria cuenta = CuentaBancaria.abrir("A", MontoDinero.de("100", "USD"));

        cuenta.debitar(MontoDinero.de("40", "USD"));

        assertThat(cuenta.saldo()).isEqualTo(MontoDinero.de("60", "USD"));
    }

    @Test
    void acreditarAumentaElSaldo() {
        CuentaBancaria cuenta = CuentaBancaria.abrir("B", MontoDinero.cero("USD"));

        cuenta.acreditar(MontoDinero.de("50", "USD"));

        assertThat(cuenta.saldo()).isEqualTo(MontoDinero.de("50", "USD"));
    }

    @Test
    void debitarSinFondosLanzaIllegalStateYNoModificaSaldo() {
        CuentaBancaria cuenta = CuentaBancaria.abrir("A", MontoDinero.de("10", "USD"));

        assertThatThrownBy(() -> cuenta.debitar(MontoDinero.de("10.01", "USD")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fondos insuficientes");
        assertThat(cuenta.saldo()).isEqualTo(MontoDinero.de("10", "USD"));
    }

    @Test
    void rechazaOperacionesConMontoCero() {
        CuentaBancaria cuenta = CuentaBancaria.abrir("A", MontoDinero.de("10", "USD"));

        assertThatThrownBy(() -> cuenta.acreditar(MontoDinero.cero("USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaTitularVacio() {
        assertThatThrownBy(() -> CuentaBancaria.abrir(" ", MontoDinero.cero("USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
