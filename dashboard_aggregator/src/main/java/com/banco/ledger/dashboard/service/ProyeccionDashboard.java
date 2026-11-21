package com.banco.ledger.dashboard.service;

import com.banco.ledger.dashboard.dto.FraudAlertRecord;
import com.banco.ledger.dashboard.dto.TransactionEventRecord;
import com.banco.ledger.dashboard.model.TransaccionVista;
import com.banco.ledger.dashboard.model.VistaMaterializada;
import com.banco.ledger.dashboard.websocket.DifusorDashboard;

import org.springframework.stereotype.Service;
import java.util.Optional;

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
