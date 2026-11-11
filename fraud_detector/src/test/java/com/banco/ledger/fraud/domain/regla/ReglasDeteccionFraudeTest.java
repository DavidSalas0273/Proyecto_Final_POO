package com.banco.ledger.fraud.domain.regla;

import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.banco.ledger.fraud.domain.model.HistorialTransacciones;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasDeteccionFraudeTest {

    private static final Instant T0 = Instant.parse("2026-11-11T11:31:00Z");

    static TransactionEventRecord evento(String txId, String monto, Instant momento) {
        return new TransactionEventRecord(txId, "A", "B", new BigDecimal(monto), "USD", "COMPLETED", null, momento, "TRANSFER");
    }

    @Test
    void altoValorDisparaAlertaSoloPorEncimaDelUmbral() {
        // Arrange
        TransaccionAltoValorRegla regla = new TransaccionAltoValorRegla(new BigDecimal("10000"));
        HistorialTransacciones historial = new HistorialTransacciones(Duration.ofMinutes(1));

        // Act & Assert
        assertThat(regla.evaluar(evento("tx-1", "50", T0), historial)).isEmpty();
        assertThat(regla.evaluar(evento("tx-2", "10000", T0), historial)).isEmpty();
        assertThat(regla.evaluar(evento("tx-3", "20000", T0), historial))
                .contains(TransaccionAltoValorRegla.MOTIVO);
    }

    @Test
    void frecuenciaAnomalaDetectaRafagasDentroDeLaVentana() {
        // Arrange
        HistorialTransacciones historial = new HistorialTransacciones(Duration.ofSeconds(60));
        FrecuenciaAnomalaRegla regla = new FrecuenciaAnomalaRegla(3, Duration.ofSeconds(60));
        for (int i = 0; i < 3; i++) {
            historial.registrar("A", T0.plusSeconds(i));
        }
        TransactionEventRecord tercera = evento("tx-3", "10", T0.plusSeconds(2));

        // Act & Assert
        assertThat(regla.evaluar(tercera, historial)).isEmpty();

        historial.registrar("A", T0.plusSeconds(3));
        assertThat(regla.evaluar(evento("tx-4", "10", T0.plusSeconds(3)), historial))
                .contains(FrecuenciaAnomalaRegla.MOTIVO);
    }

    @Test
    void frecuenciaIgnoraTransaccionesFueraDeLaVentana() {
        HistorialTransacciones historial = new HistorialTransacciones(Duration.ofSeconds(60));
        FrecuenciaAnomalaRegla regla = new FrecuenciaAnomalaRegla(1, Duration.ofSeconds(60));
        historial.registrar("A", T0);
        historial.registrar("A", T0.plusSeconds(120));

        assertThat(regla.evaluar(evento("tx", "10", T0.plusSeconds(120)), historial)).isEmpty();
    }

    @Test
    void politicaDevuelveElPrimerMotivoSegunElOrden() {
        HistorialTransacciones historial = new HistorialTransacciones(Duration.ofSeconds(60));
        historial.registrar("A", T0);
        historial.registrar("A", T0);
        PoliticaDeteccionFraude politica = new PoliticaDeteccionFraude(List.of(
                new TransaccionAltoValorRegla(new BigDecimal("10000")),
                new FrecuenciaAnomalaRegla(1, Duration.ofSeconds(60))));

        assertThat(politica.primerMotivoDeAlerta(evento("tx", "20000", T0), historial))
                .contains(TransaccionAltoValorRegla.MOTIVO);
        assertThat(politica.primerMotivoDeAlerta(evento("tx", "5", T0), historial))
                .contains(FrecuenciaAnomalaRegla.MOTIVO);
    }

    @Test
    void reglasRechazanConfiguracionInvalida() {
        assertThatThrownBy(() -> new TransaccionAltoValorRegla(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FrecuenciaAnomalaRegla(0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
