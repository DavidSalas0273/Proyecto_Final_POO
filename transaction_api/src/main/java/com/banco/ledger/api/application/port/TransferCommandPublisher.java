package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.ComandoLedger;

public interface TransferCommandPublisher {
    void publicar(ComandoLedger comando);
}
