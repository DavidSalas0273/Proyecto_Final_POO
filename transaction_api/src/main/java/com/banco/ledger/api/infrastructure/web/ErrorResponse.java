package com.banco.ledger.api.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Error de la API. field indica qué campo del formulario causó el error (si aplica) y
 * locked_until cuándo termina un bloqueo por intentos fallidos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(@JsonProperty("error") String error,
                            @JsonProperty("message") String message,
                            @JsonProperty("field") String field,
                            @JsonProperty("locked_until") Instant lockedUntil) {

    public ErrorResponse(String error, String message) {
        this(error, message, null, null);
    }
}
