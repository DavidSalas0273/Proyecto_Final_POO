package com.banco.ledger.api.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Comando publicado en la Work Queue transfer_commands. Se serializa con un campo
 * "type" (TRANSFER o DEPOSIT) para que el procesador sepa qué movimiento aplicar.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransferCommandRecord.class, name = "TRANSFER"),
        @JsonSubTypes.Type(value = DepositCommandRecord.class, name = "DEPOSIT")
})
public sealed interface ComandoLedger permits TransferCommandRecord, DepositCommandRecord {

    String txId();
}
