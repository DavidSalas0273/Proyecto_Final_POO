package com.banco.ledger.api.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferAcceptedResponse(@JsonProperty("tx_id") String txId,
                                       @JsonProperty("status") String status) {
}
