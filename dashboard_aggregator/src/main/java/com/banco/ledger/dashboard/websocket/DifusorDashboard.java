package com.banco.ledger.dashboard.websocket;

import com.banco.ledger.dashboard.model.TransaccionVista;

public interface DifusorDashboard {
    void difundir(TransaccionVista transaccion);
}
