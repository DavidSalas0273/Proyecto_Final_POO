package com.banco.ledger.processor.dto;

import com.banco.ledger.processor.model.Transaccion;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type",
        defaultImpl = TransferCommandRecord.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransferCommandRecord.class, name = "TRANSFER"),
        @JsonSubTypes.Type(value = DepositCommandRecord.class, name = "DEPOSIT")
})
public sealed interface ComandoLedger permits TransferCommandRecord, DepositCommandRecord {
    String txId();

    Transaccion crearTransaccion();
}
