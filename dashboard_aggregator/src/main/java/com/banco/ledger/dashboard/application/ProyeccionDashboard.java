package com.banco.ledger.dashboard.application;

import com.banco.ledger.dashboard.application.port.DifusorDashboard;
import com.banco.ledger.dashboard.domain.contract.FraudAlertRecord;
import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;
import com.banco.ledger.dashboard.domain.model.TransaccionVista;
import com.banco.ledger.dashboard.domain.model.VistaMaterializada;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Caso de uso: proyectar los eventos de Kafka y difundir los cambios por WebSocket. */
@Service
public class ProyeccionDashboard {

    private final VistaMaterializada vista;
    private final DifusorDashboard difusor;

    public ProyeccionDashboard(VistaMaterializada vista, DifusorDashboard difusor) {
        this.vista = vista;
        this.difusor = difusor;
    }

    public TransaccionVista alRecibirTransaccion(TransactionEventRecord evento) {
        TransaccionVista actualizada = vista.aplicarTransaccion(evento);
        difusor.difundir(actualizada);
        return actualizada;
    }

    public Optional<TransaccionVista> alRecibirAlerta(FraudAlertRecord alerta) {
        Optional<TransaccionVista> actualizada = vista.aplicarAlerta(alerta);
        actualizada.ifPresent(difusor::difundir);
        return actualizada;
    }
}
