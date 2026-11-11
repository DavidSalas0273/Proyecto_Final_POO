package com.banco.ledger.processor.application.port;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;

/** Puerto de mensajería: publica eventos en el ledger (Kafka) sin acoplarse al SDK. */
public interface PublicadorEventosLedger {

    void publicar(TransactionEventRecord evento);
}
