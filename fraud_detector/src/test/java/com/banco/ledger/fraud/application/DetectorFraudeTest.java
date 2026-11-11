package com.banco.ledger.fraud.application;

import com.banco.ledger.fraud.application.port.PublicadorAlertasFraude;
import com.banco.ledger.fraud.application.port.RepositorioAlertasFraude;
import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;
import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.banco.ledger.fraud.domain.model.HistorialTransacciones;
import com.banco.ledger.fraud.domain.regla.PoliticaDeteccionFraude;
import com.banco.ledger.fraud.domain.regla.TransaccionAltoValorRegla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DetectorFraudeTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T11:31:04Z");

    @Mock
    private RepositorioAlertasFraude repositorio;
    @Mock
    private PublicadorAlertasFraude publicador;

    private DetectorFraude detector;

    @BeforeEach
    void setUp() {
        PoliticaDeteccionFraude politica = new PoliticaDeteccionFraude(
                List.of(new TransaccionAltoValorRegla(new BigDecimal("10000"))));
        detector = new DetectorFraude(politica, new HistorialTransacciones(Duration.ofMinutes(1)),
                repositorio, publicador, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static TransactionEventRecord evento(String monto, String estado) {
        return evento(monto, estado, "TRANSFER");
    }

    private static TransactionEventRecord evento(String monto, String estado, String tipo) {
        return new TransactionEventRecord("tx-1", "A", "B", new BigDecimal(monto), "USD", estado, null,
                Instant.parse("2026-11-11T11:31:02Z"), tipo);
    }

    @Test
    void transaccionDeAltoValorSeAuditaYPublicaUnaVez() {
        // Act
        Optional<FraudAlertRecord> alerta = detector.analizar(evento("20000", "COMPLETED"));

        // Assert
        assertThat(alerta).isPresent();
        ArgumentCaptor<FraudAlertRecord> captor = ArgumentCaptor.forClass(FraudAlertRecord.class);
        verify(repositorio, times(1)).registrar(captor.capture());
        verify(publicador, times(1)).publicar(captor.getValue());
        assertThat(captor.getValue().reason()).isEqualTo(TransaccionAltoValorRegla.MOTIVO);
        assertThat(captor.getValue().detectedAt()).isEqualTo(AHORA);
    }

    @Test
    void recargasNoSeAnalizanComoFraude() {
        Optional<FraudAlertRecord> alerta = detector.analizar(new TransactionEventRecord("tx-r", null, "A",
                new BigDecimal("50000"), "USD", "COMPLETED", null, Instant.parse("2026-11-11T11:31:02Z"),
                "DEPOSIT"));

        assertThat(alerta).isEmpty();
        verifyNoInteractions(repositorio, publicador);
    }

    @Test
    void transaccionNormalNoGeneraAlerta() {
        Optional<FraudAlertRecord> alerta = detector.analizar(evento("50", "COMPLETED"));

        assertThat(alerta).isEmpty();
        verify(repositorio, never()).registrar(any());
        verify(publicador, never()).publicar(any());
    }

    @Test
    void eventosNoCompletadosSeIgnoran() {
        Optional<FraudAlertRecord> alerta = detector.analizar(evento("20000", "REJECTED"));

        assertThat(alerta).isEmpty();
        verifyNoInteractions(repositorio, publicador);
    }
}
