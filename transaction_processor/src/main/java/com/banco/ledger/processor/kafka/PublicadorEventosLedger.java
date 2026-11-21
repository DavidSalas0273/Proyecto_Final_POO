package com.banco.ledger.processor.kafka;

import com.banco.ledger.processor.dto.TransactionEventRecord;

public interface PublicadorEventosLedger {
    void publicar(TransactionEventRecord evento);
}
