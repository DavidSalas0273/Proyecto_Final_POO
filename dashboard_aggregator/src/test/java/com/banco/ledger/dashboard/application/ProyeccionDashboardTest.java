package com.banco.ledger.dashboard.application;

import com.banco.ledger.dashboard.application.port.DifusorDashboard;
import com.banco.ledger.dashboard.domain.contract.FraudAlertRecord;
import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;
import com.banco.ledger.dashboard.domain.model.TransaccionVista;
import com.banco.ledger.dashboard.domain.model.VistaMaterializada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProyeccionDashboardTest {

    @Mock
    private VistaMaterializada vista;
    @Mock
    private DifusorDashboard difusor;
    @InjectMocks
    private ProyeccionDashboard proyeccion;

    private final TransactionEventRecord evento = new TransactionEventRecord("tx-1", "A", "B",
            new BigDecimal("50"), "USD", "COMPLETED", null, Instant.parse("2026-11-11T11:30:02Z"));

    @Test
    void cadaTransaccionSeDifundeUnaVez() {
        // Arrange
        TransaccionVista fila = TransaccionVista.desde(evento);
        when(vista.aplicarTransaccion(evento)).thenReturn(fila);

        // Act
        proyeccion.alRecibirTransaccion(evento);

        // Assert
        verify(difusor, times(1)).difundir(fila);
    }

    @Test
    void alertaDeTransaccionDesconocidaNoSeDifunde() {
        FraudAlertRecord alerta = new FraudAlertRecord("tx-x", "HIGH_VALUE_TRANSACTION");
        when(vista.aplicarAlerta(alerta)).thenReturn(Optional.empty());

        proyeccion.alRecibirAlerta(alerta);

        verify(difusor, never()).difundir(any());
    }

    @Test
    void alertaDeTransaccionConocidaDifundeLaFilaEnRojo() {
        FraudAlertRecord alerta = new FraudAlertRecord("tx-1", "HIGH_VALUE_TRANSACTION");
        TransaccionVista sospechosa = TransaccionVista.desde(evento).marcarSospechosa("HIGH_VALUE_TRANSACTION");
        when(vista.aplicarAlerta(alerta)).thenReturn(Optional.of(sospechosa));

        proyeccion.alRecibirAlerta(alerta);

        verify(difusor, times(1)).difundir(sospechosa);
    }
}
