package com.banco.ledger.processor.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MontoDineroTest {

    @Test
    void normalizaEscalaYDivisa() {
        // Arrange & Act
        MontoDinero monto = new MontoDinero(new BigDecimal("50"), "usd");

        // Assert
        assertThat(monto.valor()).isEqualTo(new BigDecimal("50.00"));
        assertThat(monto.divisa()).isEqualTo("USD");
    }

    @Test
    void rechazaValoresNegativos() {
        assertThatThrownBy(() -> MontoDinero.de("-0.01", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void rechazaDivisaNulaONoAdmitida() {
        assertThatThrownBy(() -> new MontoDinero(BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MontoDinero.de("1", "XYZ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sumaYRestaDevuelvenNuevasInstancias() {
        // Arrange
        MontoDinero cien = MontoDinero.de("100", "USD");
        MontoDinero treinta = MontoDinero.de("30", "USD");

        // Act
        MontoDinero suma = cien.sumar(treinta);
        MontoDinero resta = cien.restar(treinta);

        // Assert
        assertThat(suma).isEqualTo(MontoDinero.de("130", "USD"));
        assertThat(resta).isEqualTo(MontoDinero.de("70", "USD"));
        assertThat(cien).isEqualTo(MontoDinero.de("100", "USD"));
    }

    @Test
    void noPermiteOperarEntreDivisasDistintas() {
        assertThatThrownBy(() -> MontoDinero.de("1", "USD").sumar(MontoDinero.de("1", "EUR")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restaQueProduceNegativoLanzaExcepcion() {
        assertThatThrownBy(() -> MontoDinero.de("10", "USD").restar(MontoDinero.de("11", "USD")))
                .isInstanceOf(IllegalStateException.class);
    }
}
