package com.banco.ledger.processor.application.port;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;

public interface PublicadorEventosLedger {
    void publicar(TransactionEventRecord evento);
}
