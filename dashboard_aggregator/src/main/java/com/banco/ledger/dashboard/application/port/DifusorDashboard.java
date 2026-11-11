package com.banco.ledger.dashboard.application.port;

import com.banco.ledger.dashboard.domain.model.TransaccionVista;

/** Puerto de salida: envía actualizaciones a los clientes del dashboard. */
public interface DifusorDashboard {

    void difundir(TransaccionVista transaccion);
}
