package com.banco.ledger.processor.domain.contract;

import com.banco.ledger.processor.domain.model.Transaccion;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Comando consumido desde la Work Queue transfer_commands. El campo JSON "type"
 * indica la variante; sin "type" se asume TRANSFER (compatibilidad hacia atrás).
 * Cada comando sabe construir su propio agregado (polimorfismo, sin switch).
 */
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
