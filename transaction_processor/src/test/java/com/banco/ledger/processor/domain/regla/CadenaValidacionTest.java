package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CadenaValidacionTest {

    private static final Instant T0 = Instant.parse("2026-11-11T11:30:00Z");

    private final CadenaValidacion cadena = new CadenaValidacion(List.of(
            new DivisaCompatibleRegla(),
            new LimitePorTransaccionRegla(new BigDecimal("1000")),
            new FondosSuficientesRegla()));

    private final Optional<CuentaBancaria> origen =
            Optional.of(CuentaBancaria.abrir("A", MontoDinero.de("500", "USD")));
    private final CuentaBancaria destino = CuentaBancaria.abrir("B", MontoDinero.cero("USD"));

    private static Transaccion tx(String monto, String divisa) {
        return Transaccion.recibirTransferencia(new TransactionId("tx"), "A", "B", MontoDinero.de(monto, divisa), T0);
    }

    private static Transaccion recarga(String monto, String divisa) {
        return Transaccion.recibirRecarga(new TransactionId("tx-r"), "B", MontoDinero.de(monto, divisa), T0);
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

    @Test
    void recargaNuncaSeDeniegaPorFondos() {
        assertThat(cadena.primerMotivoDeRechazo(recarga("900", "USD"), Optional.empty(), destino)).isEmpty();
    }

    @Test
    void recargaRespetaDivisaYLimite() {
        assertThat(cadena.primerMotivoDeRechazo(recarga("10", "EUR"), Optional.empty(), destino))
                .contains(DivisaCompatibleRegla.MOTIVO);
        assertThat(cadena.primerMotivoDeRechazo(recarga("2000", "USD"), Optional.empty(), destino))
                .contains(LimitePorTransaccionRegla.MOTIVO);
    }
}
