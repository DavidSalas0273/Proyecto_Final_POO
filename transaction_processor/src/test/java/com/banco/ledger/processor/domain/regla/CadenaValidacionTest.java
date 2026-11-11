package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CadenaValidacionTest {

    private final CadenaValidacion cadena = new CadenaValidacion(List.of(
            new DivisaCompatibleRegla(),
            new LimitePorTransaccionRegla(new BigDecimal("1000")),
            new FondosSuficientesRegla()));

    private final CuentaBancaria origen = CuentaBancaria.abrir("A", MontoDinero.de("500", "USD"));
    private final CuentaBancaria destino = CuentaBancaria.abrir("B", MontoDinero.cero("USD"));

    private static Transaccion tx(String monto, String divisa) {
        return Transaccion.recibir(new TransactionId("tx"), "A", "B", MontoDinero.de(monto, divisa),
                Instant.parse("2026-11-11T11:30:00Z"));
    }

    @Test
    void transaccionValidaNoTieneMotivoDeRechazo() {
        assertThat(cadena.primerMotivoDeRechazo(tx("50", "USD"), origen, destino)).isEmpty();
    }

    @Test
    void detectaFondosInsuficientes() {
        assertThat(cadena.primerMotivoDeRechazo(tx("600", "USD"), origen, destino))
                .contains(Transaccion.MOTIVO_FONDOS_INSUFICIENTES);
    }

    @Test
    void detectaLimiteExcedidoAntesQueFondos() {
        assertThat(cadena.primerMotivoDeRechazo(tx("5000", "USD"), origen, destino))
                .contains(LimitePorTransaccionRegla.MOTIVO);
    }

    @Test
    void detectaDivisaIncompatibleSinEvaluarReglasPosteriores() {
        assertThat(cadena.primerMotivoDeRechazo(tx("10", "EUR"), origen, destino))
                .contains(DivisaCompatibleRegla.MOTIVO);
    }
}
