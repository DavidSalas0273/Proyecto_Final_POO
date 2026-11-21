package com.banco.ledger.api.rabbit;

import com.banco.ledger.api.dto.ComandoLedger;

public interface TransferCommandPublisher {
    void publicar(ComandoLedger comando);
}
