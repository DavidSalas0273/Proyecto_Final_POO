package com.banco.ledger.api.dto;

import com.banco.ledger.api.model.Sesion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegistroRequest(@JsonProperty("full_name") String fullName,
                           @JsonProperty("document") String document,
                           @JsonProperty("email") String email,
                           @JsonProperty("username") String username,
                           @JsonProperty("password") String password) {
        @Override
        public String toString() {
            return "RegistroRequest[username=" + username + "]";
        }
    }

    public record LoginRequest(@JsonProperty("username") String username,
                        @JsonProperty("password") String password) {
        @Override
        public String toString() {
            return "LoginRequest[username=" + username + "]";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SesionResponse(@JsonProperty("token") String token,
                          @JsonProperty("username") String username,
                          @JsonProperty("full_name") String fullName,
                          @JsonProperty("expires_at") Instant expiresAt) {
        public static SesionResponse con(Sesion sesion) {
            return new SesionResponse(sesion.token(), sesion.usuario(), sesion.nombreCompleto(), sesion.expiraEn());
        }

        public static SesionResponse sinToken(Sesion sesion) {
            return new SesionResponse(null, sesion.usuario(), sesion.nombreCompleto(), sesion.expiraEn());
        }
    }
}
