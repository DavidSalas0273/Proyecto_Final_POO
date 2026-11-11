package com.banco.ledger.api.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** DTO inmutable de entrada del endpoint POST /deposit. */
public record DepositRequest(
        @JsonProperty("user") String user,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency) {

    public DepositRequest {
        currency = (currency == null || currency.isBlank()) ? TransferRequest.DIVISA_POR_DEFECTO : currency;
    }
}
