package com.banco.ledger.api.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** DTO inmutable de entrada del endpoint POST /transfer. */
public record TransferRequest(
        @JsonProperty("from_user") String fromUser,
        @JsonProperty("to_user") String toUser,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency) {

    public static final String DIVISA_POR_DEFECTO = "USD";

    public TransferRequest {
        currency = (currency == null || currency.isBlank()) ? DIVISA_POR_DEFECTO : currency;
    }
}
