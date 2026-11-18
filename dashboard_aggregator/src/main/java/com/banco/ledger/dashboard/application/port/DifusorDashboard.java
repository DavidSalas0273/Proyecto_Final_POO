package com.banco.ledger.dashboard.application.port;

import com.banco.ledger.dashboard.domain.model.TransaccionVista;

public interface DifusorDashboard {
    void difundir(TransaccionVista transaccion);
}
